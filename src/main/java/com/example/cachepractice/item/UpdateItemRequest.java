package com.example.cachepractice.item;

import jakarta.validation.constraints.NotBlank;

public record UpdateItemRequest(
        @NotBlank String value
) {
}

