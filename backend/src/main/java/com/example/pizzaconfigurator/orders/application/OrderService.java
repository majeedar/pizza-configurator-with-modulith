package com.example.pizzaconfigurator.orders.application;

import com.example.pizzaconfigurator.basket.api.BasketCheckout;
import com.example.pizzaconfigurator.basket.api.BasketItemSnapshot;
import com.example.pizzaconfigurator.basket.api.BasketSnapshot;
import com.example.pizzaconfigurator.orders.api.OrderAccessDeniedException;
import com.example.pizzaconfigurator.orders.api.OrderApproved;
import com.example.pizzaconfigurator.orders.api.OrderCompleted;
import com.example.pizzaconfigurator.orders.api.OrderItemView;
import com.example.pizzaconfigurator.orders.api.OrderNotFoundException;
import com.example.pizzaconfigurator.orders.api.OrderPlaced;
import com.example.pizzaconfigurator.orders.api.OrderProcessingStarted;
import com.example.pizzaconfigurator.orders.api.OrderQuery;
import com.example.pizzaconfigurator.orders.api.OrderReady;
import com.example.pizzaconfigurator.orders.api.OrderStateConflictException;
import com.example.pizzaconfigurator.orders.api.OrderTransitions;
import com.example.pizzaconfigurator.orders.api.OrderView;
import com.example.pizzaconfigurator.orders.domain.IdempotencyKey;
import com.example.pizzaconfigurator.orders.domain.IllegalStateOrderTransitionException;
import com.example.pizzaconfigurator.orders.domain.Order;
import com.example.pizzaconfigurator.orders.domain.OrderItem;
import com.example.pizzaconfigurator.orders.domain.OrderStatus;
import com.example.pizzaconfigurator.orders.infrastructure.persistence.IdempotencyKeyRepository;
import com.example.pizzaconfigurator.orders.infrastructure.persistence.OrderItemRepository;
import com.example.pizzaconfigurator.orders.infrastructure.persistence.OrderRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md §7.7: creates the Order only after the customer has confirmed
 * the final price (i.e. from an already-priced, snapshotted basket — never
 * re-validates or re-prices here), enforces idempotent creation (§15.1),
 * and owns display number / pickup token / access token generation.
 */
@Service
@Transactional
public class OrderService implements OrderQuery, OrderTransitions {

    private static final List<OrderStatus> ACTIVE_STATUSES =
        List.of(OrderStatus.CONFIRMED, OrderStatus.APPROVED, OrderStatus.IN_PROCESSING, OrderStatus.READY);

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final BasketCheckout basketCheckout;
    private final DisplayNumberGenerator displayNumberGenerator;
    private final PickupTokenGenerator pickupTokenGenerator;
    private final AccessTokenGenerator accessTokenGenerator;
    private final ApplicationEventPublisher events;
    private final JsonMapper jsonMapper;

    OrderService(
        OrderRepository orders,
        OrderItemRepository orderItems,
        IdempotencyKeyRepository idempotencyKeys,
        BasketCheckout basketCheckout,
        DisplayNumberGenerator displayNumberGenerator,
        PickupTokenGenerator pickupTokenGenerator,
        AccessTokenGenerator accessTokenGenerator,
        ApplicationEventPublisher events,
        JsonMapper jsonMapper
    ) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.idempotencyKeys = idempotencyKeys;
        this.basketCheckout = basketCheckout;
        this.displayNumberGenerator = displayNumberGenerator;
        this.pickupTokenGenerator = pickupTokenGenerator;
        this.accessTokenGenerator = accessTokenGenerator;
        this.events = events;
        this.jsonMapper = jsonMapper;
    }

    public OrderCheckoutResult checkout(
        UUID basketId,
        UUID customerId,
        String customNotes,
        String fcmDeviceToken,
        String idempotencyKey
    ) {
        String requestHash = accessTokenGenerator.hash(canonicalRequest(basketId, customerId, customNotes, fcmDeviceToken));

        IdempotencyKey existing = idempotencyKeys.findById(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyConflictException(idempotencyKey);
            }
            return new OrderCheckoutResult(getOrder(existing.getOrderId()), null);
        }

        BasketSnapshot basketSnapshot = basketCheckout.getSnapshotForCheckout(basketId);

        String displayNumber = displayNumberGenerator.next();
        String pickupToken = pickupTokenGenerator.next();
        AccessTokenGenerator.Generated accessToken = customerId == null ? accessTokenGenerator.generate() : null;

        Order order = orders.save(new Order(
            displayNumber,
            basketSnapshot.total(),
            customerId,
            customNotes,
            pickupToken,
            accessToken == null ? null : accessToken.hash(),
            fcmDeviceToken,
            basketSnapshot.currency()));

        for (BasketItemSnapshot item : basketSnapshot.items()) {
            orderItems.save(new OrderItem(
                order.getOrderId(),
                item.pizzaId(),
                item.pizzaNameSnapshot(),
                item.sizeCode(),
                item.doughCode(),
                item.quantity(),
                item.modificationsJson(),
                item.unitPrice(),
                item.ruleVersion(),
                item.priceVersion()));
        }

        basketCheckout.markCheckedOut(basketId);
        idempotencyKeys.save(new IdempotencyKey(idempotencyKey, requestHash, order.getOrderId()));

        events.publishEvent(new OrderPlaced(
            order.getOrderId(), order.getDisplayNumber(), order.getCustomerId(), order.getFcmDeviceToken(),
            order.getTotalPrice(), order.getCurrency()));

        return new OrderCheckoutResult(toView(order), accessToken == null ? null : accessToken.rawToken());
    }

    @Transactional(readOnly = true)
    public OrderView getGuardedStatus(String displayNumber, UUID requestingCustomerId, String rawAccessToken) {
        Order order = getOrderEntity(displayNumber);
        if (order.getCustomerId() != null) {
            if (!order.getCustomerId().equals(requestingCustomerId)) {
                throw new OrderAccessDeniedException();
            }
        } else {
            if (rawAccessToken == null || !accessTokenGenerator.hash(rawAccessToken).equals(order.getAccessTokenHash())) {
                throw new OrderAccessDeniedException();
            }
        }
        return toView(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getOrder(UUID orderId) {
        return toView(orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId)));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getOrderByDisplayNumber(String displayNumber) {
        return toView(getOrderEntity(displayNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderView> findActiveOrders() {
        return orders.findByStatusInOrderByCreatedAtAsc(ACTIVE_STATUSES).stream().map(this::toView).toList();
    }

    @Override
    public OrderView approve(UUID orderId) {
        Order order = transition(orderId, OrderStatus.APPROVED);
        events.publishEvent(new OrderApproved(
            order.getOrderId(), order.getDisplayNumber(), order.getCustomerId(), order.getFcmDeviceToken()));
        return toView(order);
    }

    @Override
    public OrderView start(UUID orderId) {
        Order order = transition(orderId, OrderStatus.IN_PROCESSING);
        events.publishEvent(new OrderProcessingStarted(
            order.getOrderId(), order.getDisplayNumber(), order.getCustomerId(), order.getFcmDeviceToken()));
        return toView(order);
    }

    @Override
    public OrderView markReady(UUID orderId) {
        Order order = transition(orderId, OrderStatus.READY);
        events.publishEvent(new OrderReady(
            order.getOrderId(), order.getDisplayNumber(), order.getCustomerId(), order.getFcmDeviceToken()));
        return toView(order);
    }

    @Override
    public OrderView complete(UUID orderId) {
        Order order = transition(orderId, OrderStatus.COMPLETED);
        events.publishEvent(new OrderCompleted(
            order.getOrderId(), order.getDisplayNumber(), order.getCustomerId(), order.getFcmDeviceToken()));
        return toView(order);
    }

    private Order transition(UUID orderId, OrderStatus target) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus from = order.getStatus();
        try {
            order.transitionTo(target);
        } catch (IllegalStateOrderTransitionException e) {
            throw new OrderStateConflictException(orderId, from, target);
        }
        return order;
    }

    private Order getOrderEntity(String displayNumber) {
        return orders.findByDisplayNumber(displayNumber).orElseThrow(() -> new OrderNotFoundException(displayNumber));
    }

    private String canonicalRequest(UUID basketId, UUID customerId, String customNotes, String fcmDeviceToken) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("basketId", basketId == null ? null : basketId.toString());
        canonical.put("customerId", customerId == null ? null : customerId.toString());
        canonical.put("customNotes", customNotes);
        canonical.put("fcmDeviceToken", fcmDeviceToken);
        return jsonMapper.writeValueAsString(canonical);
    }

    private OrderView toView(Order order) {
        List<OrderItemView> itemViews = orderItems.findByOrderId(order.getOrderId()).stream()
            .map(this::toItemView)
            .toList();
        return new OrderView(
            order.getOrderId(), order.getDisplayNumber(), order.getStatus(), order.getTotalPrice(), order.getCurrency(),
            order.getCustomerId(), order.getCustomNotes(), order.getPickupToken(), itemViews, order.getCreatedAt());
    }

    private OrderItemView toItemView(OrderItem item) {
        return new OrderItemView(
            item.getOrderItemId(), item.getPizzaId(), item.getPizzaNameSnapshot(), item.getSizeCode(), item.getDoughCode(),
            item.getQuantity(), item.getModificationsJson(), item.getUnitPrice(), item.getSubtotal());
    }
}
