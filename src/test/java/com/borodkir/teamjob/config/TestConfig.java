package com.borodkir.teamjob.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@TestConfiguration
@EnableJpaAuditing
public class TestConfig {

    @Bean
    @Primary
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("test-user");
    }
} 