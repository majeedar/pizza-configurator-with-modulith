package com.example.pizzaconfigurator.basket.application;

import com.example.pizzaconfigurator.basket.api.BasketAlreadyCheckedOutException;
import com.example.pizzaconfigurator.basket.api.BasketCheckout;
import com.example.pizzaconfigurator.basket.api.BasketEmptyException;
import com.example.pizzaconfigurator.basket.api.BasketItemSnapshot;
import com.example.pizzaconfigurator.basket.api.BasketNotFoundException;
import com.example.pizzaconfigurator.basket.api.BasketSnapshot;
import com.example.pizzaconfigurator.basket.domain.Basket;
import com.example.pizzaconfigurator.basket.domain.BasketItem;
import com.example.pizzaconfigurator.basket.domain.BasketStatus;
import com.example.pizzaconfigurator.basket.infrastructure.persistence.BasketItemRepository;
import com.example.pizzaconfigurator.basket.infrastructure.persistence.BasketRepository;
import com.example.pizzaconfigurator.catalog.api.CatalogQuery;
import com.example.pizzaconfigurator.catalog.api.PizzaView;
import com.example.pizzaconfigurator.configuration.api.ConfigurationQuery;
import com.example.pizzaconfigurator.configuration.api.ConfigurationSessionView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Agent.md §7.6: add priced valid configurations, update quantity, remove
 * item, aggregate from immutable snapshots. Never bypasses validation or
 * pricing — enforced by requiring {@link ConfigurationSessionView#isReadyForCheckout()}.
 */
@Service
@Transactional
public class BasketService implements BasketCheckout {

    private final BasketRepository baskets;
    private final BasketItemRepository items;
    private final ConfigurationQuery configurationQuery;
    private final CatalogQuery catalogQuery;
    private final JsonMapper jsonMapper;

    BasketService(
        BasketRepository baskets,
        BasketItemRepository items,
        ConfigurationQuery configurationQuery,
        CatalogQuery catalogQuery,
        JsonMapper jsonMapper
    ) {
        this.baskets = baskets;
        this.items = items;
        this.configurationQuery = configurationQuery;
        this.catalogQuery = catalogQuery;
        this.jsonMapper = jsonMapper;
    }

    public BasketView createBasket(UUID customerId) {
        Basket basket = baskets.save(new Basket(customerId, UUID.randomUUID().toString()));
        return toView(basket);
    }

    @Transactional(readOnly = true)
    public BasketView getBasket(UUID basketId) {
        return toView(getBasketEntity(basketId));
    }

    public BasketView addItem(UUID basketId, UUID configurationId, int quantity) {
        Basket basket = getBasketEntity(basketId);
        ConfigurationSessionView session = configurationQuery.getSession(configurationId);
        if (!session.isReadyForCheckout()) {
            throw new ConfigurationNotReadyException(configurationId);
        }
        PizzaView pizza = catalogQuery.getPizza(session.pizzaId());
        String modificationsJson = jsonMapper.writeValueAsString(Map.of(
            "removedIngredientCodes", session.removedIngredientCodes(),
            "extras", session.extras()));

        items.save(new BasketItem(
            basket.getBasketId(),
            configurationId,
            quantity,
            pizza.pizzaId(),
            pizza.code(),
            pizza.name(),
            session.sizeCode(),
            session.doughCode(),
            modificationsJson,
            session.ruleVersion(),
            session.priceVersion(),
            session.calculatedPrice(),
            session.currency()));
        return toView(basket);
    }

    public BasketView removeItem(UUID basketId, UUID basketItemId) {
        Basket basket = getBasketEntity(basketId);
        items.deleteById(basketItemId);
        return toView(basket);
    }

    @Override
    @Transactional(readOnly = true)
    public BasketSnapshot getSnapshotForCheckout(UUID basketId) {
        Basket basket = getBasketEntity(basketId);
        if (basket.getStatus() == BasketStatus.CHECKED_OUT) {
            throw new BasketAlreadyCheckedOutException(basketId);
        }
        List<BasketItemSnapshot> itemSnapshots = items.findByBasketId(basketId).stream()
            .map(this::toItemSnapshot)
            .toList();
        if (itemSnapshots.isEmpty()) {
            throw new BasketEmptyException(basketId);
        }
        BigDecimal total = itemSnapshots.stream()
            .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BasketSnapshot(basketId, itemSnapshots, total, itemSnapshots.get(0).currency());
    }

    @Override
    public void markCheckedOut(UUID basketId) {
        Basket basket = getBasketEntity(basketId);
        basket.markCheckedOut();
    }

    private Basket getBasketEntity(UUID basketId) {
        return baskets.findById(basketId).orElseThrow(() -> new BasketNotFoundException(basketId));
    }

    private BasketView toView(Basket basket) {
        List<BasketItemView> itemViews = items.findByBasketId(basket.getBasketId()).stream()
            .map(this::toItemView)
            .toList();
        BigDecimal total = itemViews.stream().map(BasketItemView::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = itemViews.isEmpty() ? "EUR" : itemViews.get(0).currency();
        return new BasketView(basket.getBasketId(), basket.getSessionToken(), itemViews, total, currency);
    }

    private BasketItemView toItemView(BasketItem item) {
        return new BasketItemView(
            item.getBasketItemId(), item.getConfigurationId(), item.getQuantity(),
            item.getPizzaNameSnapshot(), item.getSizeCode(), item.getDoughCode(),
            item.getSnapshotPrice(), item.getSnapshotCurrency(), item.lineTotal());
    }

    private BasketItemSnapshot toItemSnapshot(BasketItem item) {
        return new BasketItemSnapshot(
            item.getBasketItemId(), item.getConfigurationId(), item.getQuantity(),
            item.getPizzaId(), item.getPizzaCode(), item.getPizzaNameSnapshot(), item.getSizeCode(), item.getDoughCode(),
            item.getModificationsJson(), item.getRuleVersion(), item.getPriceVersion(),
            item.getSnapshotPrice(), item.getSnapshotCurrency());
    }
}
