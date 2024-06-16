package com.polarbookshop.orderservice.web;


import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/orders")
@RestController
public class OrderController {

    private final OrderService orderService;


    //
    @GetMapping
    public Flux<Order> getAllOrders(){
        return orderService.getAllOrders();
    }

    @PostMapping
    public Mono<Order> submitOrder(@RequestBody @Valid OrderRequest orderRequest){
        return orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity());
    }
}
