package com.polarbookshop.orderservice.domain;


import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final BookClient bookClient;

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
                .flatMap(orderRepository::save);
    }

    // 거부
    public static Order buildRejectOrder(String isbn, int quantity){
        return Order.of(isbn, null, null, quantity, OrderStatus.REJECTED);
    }
    //승인
    public static Order buildAcceptedOrder(Book book, int quantity){
        return Order.of(
                book.isbn(),
                book.title() + " - " + book.author(),
                book.price(),
                quantity,
                OrderStatus.ACCEPTED
        );
    }
}
