package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderDto;
import com.ecommerce.order_service.dto.OrderItemsDto;
import com.ecommerce.order_service.messagequeue.KafkaProducer;
import com.ecommerce.order_service.messagequeue.KafkaProducerConfig;
import com.ecommerce.order_service.service.connector.InternalServiceConnector;
import com.ecommerce.order_service.vo.ResponseCatalog;
import com.ecommerce.order_service.vo.ResponseKey;
import com.ecommerce.snowflake.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                "org.springframework.cloud.stream.config.BindingServiceConfiguration"
})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.kafka.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.datasource.driver-class-name=org.mariadb.jdbc.Driver",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.cloud.bus.enabled=false",
        "spring.cloud.stream.enabled=false",
        "spring.cloud.stream.kafka.binder.enabled=false",
        "spring.rabbitmq.enabled=false"
})
@AutoConfigureWireMock(port = 0)
@Testcontainers
class OrderServiceImplShardingTest {
    @Autowired
    private OrderServiceImpl orderService;

    @MockitoBean
    private InternalServiceConnector internalConnector;

    @Autowired
    private DataSource shard0DataSource;
    @Autowired
    private DataSource shard1DataSource;

    @MockitoBean
    private KafkaProducerConfig kafkaProducerConfig;

    @MockitoBean
    private KafkaProducer kafkaProducer;

    @MockitoBean
    private SnowflakeIdGenerator idGenerator;

    @Container
    static MariaDBContainer<?> shard0Container = new MariaDBContainer<>("mariadb:10.6")
            .withDatabaseName("gameinfo_order")
            .withInitScript("schema.sql");

    @Container
    static MariaDBContainer<?> shard1Container = new MariaDBContainer<>("mariadb:10.6")
            .withDatabaseName("gameinfo_order2")
            .withInitScript("schema.sql");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.shard0.url", shard0Container::getJdbcUrl);
        registry.add("spring.datasource.shard1.url", shard1Container::getJdbcUrl);
        registry.add("spring.datasource.username", shard0Container::getUsername);
        registry.add("spring.datasource.password", shard0Container::getPassword);
    }

    @BeforeEach
    void setUp() {
        when(idGenerator.nextId()).thenReturn(10001L);

        ResponseCatalog catalog = new ResponseCatalog();
        catalog.setProductId("PROD-001");
        catalog.setUnitPrice(15000);
        catalog.setProductName("Test Game");
        when(internalConnector.getCatalogList(any())).thenReturn(List.of(catalog));

        ResponseKey key = new ResponseKey();
        key.setProductId("PROD-001");
        key.setGameKey("ABCDE-12345-FGHIJ");
        when(internalConnector.assignKeys(any(), any())).thenReturn(List.of(key));
    }

    @Test
    @DisplayName("주문 생성 시 샤딩 알고리즘에 따라 물리적 데이터베이스에 정상 적재")
    void shardingDistributionTest() throws Exception {
        String userId = getUserIdForShard(0);
        OrderDto orderDto = createOrderDto("PROD-001");

        OrderDto result = orderService.createOrder(orderDto, userId);
        System.out.println("결과 : " + result);

        DriverManagerDataSource ds0 = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds0.setUrl(shard0Container.getJdbcUrl());
        ds0.setUsername(shard0Container.getUsername());
        ds0.setPassword(shard0Container.getPassword());

        DriverManagerDataSource ds1 = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds1.setUrl(shard1Container.getJdbcUrl());
        ds1.setUsername(shard1Container.getUsername());
        ds1.setPassword(shard1Container.getPassword());

        JdbcTemplate jdbc0 = new JdbcTemplate(ds0);
        JdbcTemplate jdbc1 = new JdbcTemplate(ds1);

        Integer shard0Count = jdbc0.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        Integer shard1Count = jdbc1.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);

        System.out.println(">>>> [REAL DB CHECK] Shard 0: " + shard0Count);
        System.out.println(">>>> [REAL DB CHECK] Shard 1: " + shard1Count);

        assertThat(shard0Count).as("Shard 0에 데이터가 있어야 합니다.").isEqualTo(1);
        assertThat(shard1Count).as("Shard 1에는 데이터가 없어야 합니다.").isEqualTo(0);
    }

    @Test
    @DisplayName("유저 ID에 따라 데이터가 물리적으로 다른 샤드 DB에 저장되어야 한다")
    void shardingDistributionByUserIdTest() throws Exception {
        String userIdForShard0 = findUserIdForShard(0);
        createAndVerifyOrder(userIdForShard0, 0);

        String userIdForShard1 = findUserIdForShard(1);
        createAndVerifyOrder(userIdForShard1, 1);
    }

    @Test
    @DisplayName("주문 상세 저장 중 예외 발생 시 전체 트랜잭션이 롤백되어야 한다")
    void orderRollbackTest() throws Exception {
        String userId = getUserIdForShard(0);
        OrderDto orderDto = createOrderDto("PROD-001");

        when(internalConnector.assignKeys(any(), any()))
                .thenThrow(new RuntimeException("강제 서비스 장애 발생"));

        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderDto, userId);
        });

        DriverManagerDataSource ds0 = new DriverManagerDataSource();
        ds0.setUrl(shard0Container.getJdbcUrl());
        ds0.setUsername(shard0Container.getUsername());
        ds0.setPassword(shard0Container.getPassword());
        JdbcTemplate jdbc0 = new JdbcTemplate(ds0);

        Integer orderCount = jdbc0.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        Integer itemCount = jdbc0.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class);

        System.out.println(">>>> [Rollback Check] Order Count: " + orderCount);
        System.out.println(">>>> [Rollback Check] Item Count: " + itemCount);

        assertThat(orderCount).as("트랜잭션 롤백으로 인해 주문 데이터가 없어야 합니다.").isEqualTo(0);
        assertThat(itemCount).as("트랜잭션 롤백으로 인해 주문 상세 데이터가 없어야 합니다.").isEqualTo(0);
    }

    @Test
    @DisplayName("저장된 주문 데이터의 필드 값이 전달된 DTO와 정확히 일치해야 한다")
    void orderDataIntegrityTest() throws Exception {
        String userId = getUserIdForShard(0);
        String productId = "PROD-999";
        OrderDto orderDto = createOrderDto(productId);

        ResponseCatalog catalog = new ResponseCatalog();
        catalog.setProductId(productId);
        catalog.setUnitPrice(15000);
        catalog.setProductName("Test Game 999");

        when(internalConnector.getCatalogList(any())).thenReturn(List.of(catalog));

        ResponseKey key = new ResponseKey();
        key.setProductId(productId);
        key.setGameKey("ABCDE-12345-FGHIJ");

        when(internalConnector.assignKeys(any(), any())).thenReturn(List.of(key));

        OrderDto result = orderService.createOrder(orderDto, userId);

        DriverManagerDataSource ds0 = new DriverManagerDataSource();
        ds0.setUrl(shard0Container.getJdbcUrl());
        ds0.setUsername(shard0Container.getUsername());
        ds0.setPassword(shard0Container.getPassword());
        JdbcTemplate jdbc0 = new JdbcTemplate(ds0);

        var orderMap = jdbc0.queryForMap("SELECT * FROM orders WHERE user_id = ?", userId);
        assertThat(orderMap.get("user_id")).isEqualTo(userId);

        var itemMap = jdbc0.queryForMap("SELECT * FROM order_items WHERE product_id = ?", productId);
        assertThat(itemMap.get("product_id")).isEqualTo(productId);
        assertThat(itemMap.get("unit_price")).isEqualTo(15000);
    }

    private String getUserIdForShard(int targetIndex) {
        for (int i = 0; i < 100; i++) {
            String testId = "user-" + i;
            int shardIndex = Math.abs(testId.hashCode() % 2);
            if (shardIndex == targetIndex) {
                return testId;
            }
        }
        return "user-default";
    }

    private String findUserIdForShard(int targetShard) {
        for (int i = 100; i < 1000; i++) {
            String testId = "user-" + i;
            if (Math.abs(testId.hashCode() % 2) == targetShard) {
                return testId;
            }
        }
        return "user-default";
    }

    private void createAndVerifyOrder(String userId, int expectedShard) throws Exception {
        OrderDto orderDto = createOrderDto("PROD-001");
        orderService.createOrder(orderDto, userId);

        DriverManagerDataSource ds0 = new DriverManagerDataSource();
        ds0.setUrl(shard0Container.getJdbcUrl());
        ds0.setUsername(shard0Container.getUsername());
        ds0.setPassword(shard0Container.getPassword());

        DriverManagerDataSource ds1 = new DriverManagerDataSource();
        ds1.setUrl(shard1Container.getJdbcUrl());
        ds1.setUsername(shard1Container.getUsername());
        ds1.setPassword(shard1Container.getPassword());

        JdbcTemplate jdbc0 = new JdbcTemplate(ds0);
        JdbcTemplate jdbc1 = new JdbcTemplate(ds1);

        int count0 = jdbc0.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);
        int count1 = jdbc1.queryForObject("SELECT COUNT(*) FROM orders WHERE user_id = ?", Integer.class, userId);

        System.out.println(">>>> [Shard Check] User: " + userId);
        System.out.println(">>>> Shard 0 Count: " + count0 + " | Shard 1 Count: " + count1);

        if (expectedShard == 0) {
            assertThat(count0).isEqualTo(1);
            assertThat(count1).isEqualTo(0);
        } else {
            assertThat(count0).isEqualTo(0);
            assertThat(count1).isEqualTo(1);
        }
    }

    private OrderDto createOrderDto(String productId) {
        OrderDto dto = new OrderDto();
        dto.setUsedPoint(0);
        OrderItemsDto item = new OrderItemsDto();
        item.setProductId(productId);
        dto.setOrderItems(List.of(item));
        return dto;
    }
}