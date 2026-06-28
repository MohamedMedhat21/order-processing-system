package io.github.mohamedmedhat21.order_processing_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "io.github.mohamedmedhat21.order_processing_system.repository")
public class JpaConfig {
}
