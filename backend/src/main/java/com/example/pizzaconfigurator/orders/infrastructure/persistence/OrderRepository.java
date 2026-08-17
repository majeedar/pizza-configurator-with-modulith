package com.example.pizzaconfigurator.orders.infrastructure.persistence;

import com.example.pizzaconfigurator.orders.domain.Order;
import com.example.pizzaconfigurator.orders.domain.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByDisplayNumber(String displayNumber);

    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);

    @Query(value = "select nextval('orders.display_number_seq')", nativeQuery = true)
    long nextDisplayNumberSequenceValue();
}
