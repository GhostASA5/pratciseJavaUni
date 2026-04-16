package com.example.practise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "order_items")
@Getter @Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Double subtotal;
}