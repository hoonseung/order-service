package com.polarbookshop.orderservice.domain;


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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
        @LastModifiedDate
        Instant lastModifiedDate,

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
                0
        );
    }



    public static Order of(Long orderId, String bookIsbn, String bookName, Double bookPrice, Integer quantity, Instant createdDate,
                           Instant lastModifiedDate, OrderStatus orderstatus, int version) {
        return new Order(
                orderId,
                bookIsbn,
                bookName,
                bookPrice,
                quantity,
                orderstatus,
                createdDate,
                lastModifiedDate,
                version
        );
    }

}
