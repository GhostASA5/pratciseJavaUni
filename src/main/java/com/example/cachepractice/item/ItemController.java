package com.example.cachepractice.item;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ItemDto get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> put(@PathVariable long id, @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(service.put(id, request.value()));
    }
}

