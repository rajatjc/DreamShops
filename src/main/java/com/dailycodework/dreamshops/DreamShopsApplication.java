package com.dailycodework.dreamshops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

/***
 * Product to Category m:1
 * Product to images 1:n
 * User to Cart 1:1, Cart to CartItem 1:n
 * User to Order 1:n, Order to OrderItem 1:n
 */
@SpringBootApplication
public class DreamShopsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DreamShopsApplication.class, args);
	}

	/** Injected wherever the current time is recorded, so tests can fix it. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
