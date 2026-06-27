package io.github.mohamedmedhat21.order_processing_system;

import org.springframework.boot.SpringApplication;

public class TestOrderProcessingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderProcessingSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
