package ru.yandex.practicum.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
public class ShopApplication {

	public static void main(String[] args) {
		ReactorDebugAgent.init();
		SpringApplication.run(ShopApplication.class, args);
	}

}
