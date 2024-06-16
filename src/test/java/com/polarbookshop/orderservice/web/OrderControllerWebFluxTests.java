package com.polarbookshop.orderservice.web;


import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderService;
import com.polarbookshop.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;


@WebFluxTest(OrderController.class)
 class OrderControllerWebFluxTests {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private OrderService orderService;



    @Test
    void whenBookCreateNotAvailableThenRejectOrder(){
        var isbn = "1234567890";
        var orderRequest = new OrderRequest(isbn, 3);
        var expectedOrder = OrderService.buildRejectOrder(orderRequest.isbn(), orderRequest.quantity());

        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));


        webClient.post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(actualOrder -> {
                    assertThat(actualOrder).isNotNull();
                    assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                });
    }

}
