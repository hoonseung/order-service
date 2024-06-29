package com.polarbookshop.orderservice;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderService;
import com.polarbookshop.orderservice.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.web.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.Key;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // 랜덤 포트 설정해야 webTestClient 빈 주입됨
@Import(TestChannelBinderConfiguration.class)
@Testcontainers
class OrderServiceApplicationTests {

    private static KeycloakToken bjornTokens;

    private static KeycloakToken isabelleTokens;

    @MockBean
    private BookClient bookClient;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutputDestination output;

    @BeforeAll
    static void generateAccessTokens(){
       var webClient = WebClient.builder()
               .baseUrl(keycloakContainer.getAuthServerUrl() + "/realms/PolarBookshop/protocol/openid-connect/token")
               .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
               .build();

       bjornTokens = authenticationWith("bjorn", "password", webClient);
       isabelleTokens = authenticationWith("isabelle", "password", webClient);
    }


    @Container
    private static final PostgreSQLContainer<?> postgresql =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.12"));

    @Container
    private static final KeycloakContainer keycloakContainer =
            new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withRealmImportFile("test-realm-config.json");


    @DynamicPropertySource
    private static void dynamicProperty(DynamicPropertyRegistry registry){
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "/realms/PolarBookshop");
    }

    //r2dbc:postgresql://localhost:5432/polardb_order
    private static String r2dbcUrl(){
        return String.format("r2dbc:postgresql://localhost:%s/%s",
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @DisplayName("도서주문에 성공하고 해당 도서를 성공적으로 조회")
    @Test
    void whenGetOwnOrdersThenReturn() throws IOException {
        var isbn = "1234567890";
        var orderRequest = new OrderRequest("1234567890", 1);
        var book = new Book(isbn, "auth", "author", 1000.0);
        BDDMockito.given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));

        Order expectedOrder = webTestClient.post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(bjornTokens.accessToken))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth(isabelleTokens.accessToken))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> {
                    List<Long> orderId = orders.stream().map(Order::id)
                            .toList();
                    assertThat(orderId.contains(expectedOrder.id()));
                });
    }







    private static KeycloakToken authenticationWith(String username, String password, WebClient webClient){
        return webClient
                .post()
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "polar-test")
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(KeycloakToken.class)
                .block();
    }


    private record KeycloakToken(
            String accessToken
    ){

        @JsonCreator
        public KeycloakToken(@JsonProperty("access_token") final String accessToken){
            this.accessToken = accessToken;
        }
    }

}
