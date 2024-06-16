package com.polarbookshop.orderservice.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(

        @NotBlank(message = "ISBN은 필수 입력 값입니다.")
        String isbn,

        @NotNull(message = "책 수량은 필수 입니다.")
        @Min(value = 1, message = "주문 책 수량은 최소 1개이어야 합니다.")
        @Max(value = 5, message = "주문 책 수량은 최대 5개까지 입니다.")
        Integer quantity

) {
}
