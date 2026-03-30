package com.ecommerce.order_service.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    private final Environment env;

    public DataSourceConfig(Environment env) {
        this.env = env;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig commonHikariConfig() {
        return new HikariConfig();
    }

    @Bean
    public DataSource shard0DataSource() {
        return createDataSource(env.getProperty("spring.datasource.shard0.url"));
    }

    @Bean
    public DataSource shard1DataSource() {
        return createDataSource(env.getProperty("spring.datasource.shard1.url"));
    }

    private DataSource createDataSource(String url) {
        HikariConfig config = new HikariConfig();

        HikariConfig common = commonHikariConfig();
        config.setIdleTimeout(common.getIdleTimeout());
        config.setLeakDetectionThreshold(common.getLeakDetectionThreshold());
        config.setMaximumPoolSize(common.getMaximumPoolSize());

        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        return new HikariDataSource(config);
    }

    @Bean
    public DataSource routingDataSource() {
        ShardRoutingDataSource routingDataSource = new ShardRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(0, shard0DataSource());
        dataSourceMap.put(1, shard1DataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(shard0DataSource());

        return routingDataSource;
    }

    @Primary
    @Bean
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }
}
