package io.github.mohamedmedhat21.order_processing_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit.order-creation")
public record RateLimitProperties(int capacity, int refillTokens, int refillPeriodSeconds) {

	public RateLimitProperties {
		if (capacity <= 0) {
			capacity = 10;
		}
		if (refillTokens <= 0) {
			refillTokens = capacity;
		}
		if (refillPeriodSeconds <= 0) {
			refillPeriodSeconds = 60;
		}
	}
}
