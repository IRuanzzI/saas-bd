package br.com.ruan_dev.saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaasApplication.class, args);
	}

}
