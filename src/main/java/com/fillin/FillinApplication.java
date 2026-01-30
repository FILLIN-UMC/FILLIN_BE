package com.fillin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EntityScan("com.fillin.domain")
public class FillinApplication {

	public static void main(String[] args) {
		SpringApplication.run(FillinApplication.class, args);
	}

}
