package com.StocksMCP.demo;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StockApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(StockApplication.class);

		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@Bean
	public ToolCallbackProvider stockTools(StockService stockService) {
		return  MethodToolCallbackProvider.builder().toolObjects(stockService).build();
	}
}
