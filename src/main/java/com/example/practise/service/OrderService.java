package com.example.practise.service;

import com.example.practise.dto.OrderItemRequest;
import com.example.practise.model.Order;
import com.example.practise.model.OrderItem;
import com.example.practise.model.Product;
import com.example.practise.repository.OrderItemRepository;
import com.example.practise.repository.OrderRepository;
import com.example.practise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Order createOrder(Long customerId, List<OrderItemRequest> items) {

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmount(0.0);
        order = orderRepository.save(order);

        double total = 0;

        for (OrderItemRequest req : items) {

            Product product = productRepository.findById(req.getProductId())
                    .orElseThrow();

            double subtotal = product.getPrice() * req.getQuantity();

            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setProductId(product.getProductId());
            item.setQuantity(req.getQuantity());
            item.setSubtotal(subtotal);

            orderItemRepository.save(item);

            total += subtotal;
        }

        // 3. обновляем totalAmount
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }
}