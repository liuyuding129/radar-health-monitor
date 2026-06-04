package com.lz.radar.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DruidConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource.druid.master")
    @Primary
    public DataSource masterDataSource() {
        return DruidDataSourceBuilder.create().build();
    }
}