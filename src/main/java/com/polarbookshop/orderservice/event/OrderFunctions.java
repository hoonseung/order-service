package com.polarbookshop.orderservice.event;


import com.polarbookshop.orderservice.domain.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Slf4j
@Configuration
public class OrderFunctions {



    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService){
         return flux -> orderService.consumeOrderDispatchedEvent(flux) // 발송된 주문에 대해 업데이트
                .doOnNext(order -> log.info("id {}order has dispatched" ,order.id()))
                .subscribe();// 리엑티브 스트림을 활성화하기 위해 구독한다 구독자가 없으면 스트림을 통해 데이터가 흐르지 않는다
    }

}
