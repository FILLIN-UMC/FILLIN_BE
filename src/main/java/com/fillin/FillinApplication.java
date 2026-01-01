package com.fillin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan("com.fillin.domain")
public class FillinApplication {

	public static void main(String[] args) {
		SpringApplication.run(FillinApplication.class, args);
	}

}
