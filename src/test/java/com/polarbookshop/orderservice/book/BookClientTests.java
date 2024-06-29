package com.polarbookshop.orderservice.book;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

 class BookClientTests {

    private MockWebServer mockWebServer;

    private BookClient bookClient;


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        bookClient = new BookClient(WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build());
    }

    @AfterEach
    void clean() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    void whenBookExistsThenReturnBook(){
        var bookIsbn = "1234667876";

        var mockResponse = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn" : %s,
                            "title" : "Title",
                            "author" : "Author",
                            "price" : 9.90,
                            "publisher" : "Polarsophia"
                        }
                        """.formatted(bookIsbn));

        mockWebServer.enqueue(mockResponse);
        Mono<Book> book = bookClient.getBookByIsbn(bookIsbn);

        // 리액터의 유틸리티 테스터
        // 리액티브 스트림을 처리하고 플루언트 api를 통해 단계별로 실행해 각각의 작동을 테스트 가능
        StepVerifier.create(book)
                .expectNextMatches(
                        b -> b.isbn().equals(bookIsbn)
                ).verifyComplete();

    }
}
