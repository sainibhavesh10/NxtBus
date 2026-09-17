package com.nxtbus.backend.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "Search query must not be blank")
        @Size(min = 3, message = "Search query must be at least 3 characters")
        String query,

        @Min(value = 1, message = "Limit must be at least 1")
        Integer limit
) {
    public SearchRequest {
        if (limit == null) {
            limit = 10;
        }
    }
}