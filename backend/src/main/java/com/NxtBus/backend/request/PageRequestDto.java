package com.nxtbus.backend.request;

import jakarta.validation.constraints.Min;

public record PageRequestDto(
        @Min(value = 0, message = "page must be >= 0")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
    public PageRequestDto {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 10;
        }
    }
}