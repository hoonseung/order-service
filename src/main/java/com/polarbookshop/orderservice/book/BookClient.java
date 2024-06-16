package com.polarbookshop.orderservice.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class BookClient {

    private static final String BOOKS_ROOT_API = "/books/";

    private final WebClient webClient;



    public Mono<Book> getBookByIsbn(String isbn){
        return webClient
                .get()
                .uri(BOOKS_ROOT_API + isbn) // baseUri/books/isbn
                .retrieve()// 요청 받고 응답 받음
                .bodyToMono(Book.class)// 받은 객체를 Mono<Book> 으로 반환
                .timeout(Duration.ofSeconds(3), Mono.empty())// 요청에 대해 3초타임아웃 설정 타임아웃 시 빈 MONO 객체로 대체
                .onErrorResume(WebClientResponseException.class, exception -> Mono.empty())
                .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                .onErrorResume(Exception.class,
                        exception -> Mono.empty());
    }


}
