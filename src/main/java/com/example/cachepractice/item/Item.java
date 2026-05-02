package com.example.cachepractice.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "items")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Item {

    @Id
    private Long id;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false)
    private Instant updatedAt;

    public Item(Long id, String value, Instant updatedAt) {
        this.id = id;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public void updateValue(String value, Instant updatedAt) {
        this.value = value;
        this.updatedAt = updatedAt;
    }
}

