package io.github.mohamedmedhat21.order_processing_system.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean
	Executor virtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}
