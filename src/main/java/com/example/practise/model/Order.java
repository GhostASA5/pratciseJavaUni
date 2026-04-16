package com.example.practise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "orders")
@Getter @Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long customerId;
    private LocalDateTime orderDate;
    private Double totalAmount;
}