package com.polarbookshop.orderservice.domain;


import com.polarbookshop.orderservice.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.event.OrderDispatchedMessage;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;


@Slf4j
@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private final BookClient bookClient;

    private final StreamBridge streamBridge; // 애플리케이션의 REST 계층과 스트림을 연결하여 특정 대상에게 데이터를 보낼 수 있다.

    // 여러 개의 객체를 위해 사용
    // 0개 이상의 주문에 대한 비동기 시퀀스를 나타내는 FLUX
    public Flux<Order> getAllOrders(){
        return orderRepository.findAll();
    }


    // map은 표준 자바 유형 사이를 맵핑 Order<Mono<Order>>
    // flatmap은 자바 유형을 리액티브 스트림으로 매핑한다. Mono<Order>
    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity){
        return bookClient.getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectOrder(isbn, quantity))
                .flatMap(orderRepository::save) // 주문 데이터 저장
                .doOnNext(this::publishOrderAcceptedEvent); // 주문 수락 알림 발송
    }

    // 거부
    public static Order buildRejectOrder(String isbn, int quantity){
        return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
    }
    //승인
    private static Order buildAcceptedOrder(Book book, int quantity){
        return Order.of(
                book.isbn(),
                book.title() + " - " + book.author(),
                book.price(),
                quantity,
                OrderStatus.ACCEPTED
        );
    }

    // 주문 수락 이벤트 발생
    private void publishOrderAcceptedEvent(Order order){
        if (!order.status().equals(OrderStatus.ACCEPTED)){
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Order has been accepted. id: {}", order.id());
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Order data transmission result id: {} transfer state: {}", order.id(), result);
    }



    // 발송된 주문에 대해 해당 주문 업데이트
    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux){
        return flux.flatMap(orderDispatchedMessage -> orderRepository.findById(orderDispatchedMessage.orderId())
                        .timeout(Duration.ofSeconds(2))
                        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                                .onErrorResume(Exception.class, exception -> Mono.empty())
                        )
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }
    private Order buildDispatchedOrder(Order existingOrder){
        return Order.of(
                existingOrder.id(),
                existingOrder.bookIsbn(),
                existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(),
                existingOrder.createdDate(),
                existingOrder.lastModifiedDate(),
                OrderStatus.DISPATCHED,
                existingOrder.version()
        );
    }
}
