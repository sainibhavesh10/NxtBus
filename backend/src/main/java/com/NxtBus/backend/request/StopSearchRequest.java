package com.nxtbus.backend.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StopSearchRequest(
        @NotBlank(message = "Search query must not be blank")
        @Size(min = 3, message = "Search query must be at least 3 characters")
        String query,

        @Min(value = 1, message = "Limit must be at least 1")
        int limit
) {
    public StopSearchRequest {
        if (limit == 0) {
            limit = 3;
        }
    }
}
