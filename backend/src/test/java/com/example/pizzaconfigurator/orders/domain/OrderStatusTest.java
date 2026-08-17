package com.example.pizzaconfigurator.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void happyPathTransitionsAreAllowed() {
        Order order = newConfirmedOrder();
        order.transitionTo(OrderStatus.APPROVED);
        order.transitionTo(OrderStatus.IN_PROCESSING);
        order.transitionTo(OrderStatus.READY);
        order.transitionTo(OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void terminalStatesAcceptNoFurtherTransitions() {
        Order order = newConfirmedOrder();
        order.transitionTo(OrderStatus.APPROVED);
        order.transitionTo(OrderStatus.IN_PROCESSING);
        order.transitionTo(OrderStatus.READY);
        order.transitionTo(OrderStatus.COMPLETED);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
            .isInstanceOf(IllegalStateOrderTransitionException.class);
    }

    @Test
    void rejectionIsOnlyAllowedBeforeProcessingStarts() {
        Order order = newConfirmedOrder();
        assertThatThrownBy(() -> {
            order.transitionTo(OrderStatus.APPROVED);
            order.transitionTo(OrderStatus.IN_PROCESSING);
            order.transitionTo(OrderStatus.REJECTED);
        }).isInstanceOf(IllegalStateOrderTransitionException.class);
    }

    @Test
    void invalidReverseTransitionIsRejected() {
        Order order = newConfirmedOrder();
        order.transitionTo(OrderStatus.APPROVED);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMED))
            .isInstanceOf(IllegalStateOrderTransitionException.class);
    }

    private Order newConfirmedOrder() {
        Order order = new Order(
            "P-00001", new BigDecimal("10.00"), null, null, "ABC123", "somehash", null, "EUR");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        return order;
    }
}
