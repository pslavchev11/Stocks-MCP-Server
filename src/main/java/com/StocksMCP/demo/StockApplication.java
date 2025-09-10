package com.StocksMCP.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(StockApplication.class);

		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}
}
