package com.example.practise.service;

import com.example.practise.model.Customer;
import com.example.practise.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public void updateEmail(Long customerId, String newEmail) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow();

        customer.setEmail(newEmail);

        customerRepository.save(customer);
    }
}