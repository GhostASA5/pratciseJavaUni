package com.example.practise.api;

import com.example.practise.dto.OrderItemRequest;
import com.example.practise.model.Order;
import com.example.practise.model.Product;
import com.example.practise.service.CustomerService;
import com.example.practise.service.OrderService;
import com.example.practise.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StoreController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final ProductService productService;

    @PostMapping("/orders")
    public Order createOrder(@RequestParam Long customerId,
                             @RequestBody List<OrderItemRequest> items) {
        return orderService.createOrder(customerId, items);
    }

    @PutMapping("/customers/{id}/email")
    public void updateEmail(@PathVariable Long id,
                            @RequestParam String email) {
        customerService.updateEmail(id, email);
    }

    @PostMapping("/products")
    public Product createProduct(@RequestParam String name,
                                 @RequestParam Double price) {
        return productService.createProduct(name, price);
    }
}