package com.example.practise.service;

import com.example.practise.model.Product;
import com.example.practise.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(String name, Double price) {

        Product product = new Product();
        product.setProductName(name);
        product.setPrice(price);

        return productRepository.save(product);
    }
}