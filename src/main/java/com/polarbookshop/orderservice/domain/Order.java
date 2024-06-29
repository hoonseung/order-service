package com.polarbookshop.orderservice.domain;


import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "orders")
public record Order(

        @Id
        Long id,

        String bookIsbn,

        String bookName,

        Double bookPrice,

        Integer quantity,

        OrderStatus status,

        @CreatedDate
        Instant createdDate,

        @CreatedBy
        String createdBy,

        @LastModifiedDate
        Instant lastModifiedDate,

        @LastModifiedBy
        String lastModifiedBy,

        @Version
        int version

) {

    public static Order of(String bookIsbn, String bookName, Double bookPrice, Integer quantity, OrderStatus orderstatus){
        return new Order(
                null,
                bookIsbn,
                bookName,
                bookPrice,
                quantity,
                orderstatus,
                null,
                null,
                null,
                null,
                0
        );
    }



    public static Order of(Long orderId, String bookIsbn, String bookName, Double bookPrice, Integer quantity,
                           Instant createdDate, String createdBy, Instant lastModifiedDate, String lastModifiedBy,
                           OrderStatus orderstatus, int version) {
        return new Order(
                orderId,
                bookIsbn,
                bookName,
                bookPrice,
                quantity,
                orderstatus,
                createdDate,
                createdBy,
                lastModifiedDate,
                lastModifiedBy,
                version
        );
    }

}
