package com.dailycodework.dreamshops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/***
 * Product to Category m:1
 * Product to images 1:n
 */
@SpringBootApplication
public class DreamShopsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DreamShopsApplication.class, args);
	}

}
