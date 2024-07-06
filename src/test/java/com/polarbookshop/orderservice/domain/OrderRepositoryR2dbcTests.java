package com.polarbookshop.orderservice.domain;


import com.polarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.util.Objects;


@Disabled
@DataR2dbcTest
@Import(DataConfig.class)
@Testcontainers
 class OrderRepositoryR2dbcTests {

    @Container
    static PostgreSQLContainer<?> postgresql =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));


    @Autowired
    private OrderRepository orderRepository;


    @DynamicPropertySource // 테스트 postgresql 인스턴스에 연결하기 위해 r2dbc 와 플라이웨이 설정을 변경한다
    static void postgresqlProperties(DynamicPropertyRegistry registry){
        registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    private static String r2dbcUrl(){
        return String.format("r2dbc:postgresql://%s:%s/%s",
                postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgresql.getDatabaseName());
    }

    @Test
    void createRejectedOrder(){
        var rejectOrder = OrderService.buildRejectOrder("1234567890", 3);

        StepVerifier.create(orderRepository.save(rejectOrder))
                .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
                .verifyComplete();
    }


    @DisplayName("미인증 시 데이터 감사 확인")
    @Test
    void whenCreateOrderNotAuthenticatedThenNoAuditMetaData(){
        var rejectOrder = OrderService.buildRejectOrder("1234567890", 1);

        StepVerifier.create(orderRepository.save(rejectOrder))
                .expectNextMatches(order -> Objects.isNull(order.createdBy()) && Objects.isNull(order.lastModifiedBy()))
                .verifyComplete();
    }


    @DisplayName("인증 시 데이터 감사 확인")
    @WithMockUser("devJohn")
    void whenCreateOrderAuthenticatedThenAuditMetaData(){
        var authUser = "devJohn";
        var rejectOrder = OrderService.buildRejectOrder("1234567890", 1);

        StepVerifier.create(orderRepository.save(rejectOrder))
                .expectNextMatches(order -> order.createdBy().equals(authUser)
                                && (order.lastModifiedBy().equals(authUser)))
                .verifyComplete();
    }
}
