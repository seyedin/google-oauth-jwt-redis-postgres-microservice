package com.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {
	private static final Logger log = LoggerFactory.getLogger(AuthApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
		log.info("\n Swagger OpenAPI documentation is available at: http://localhost:8081/swagger-ui/index.html\n");
		log.info("\n The Spring Boot Application is now up and running on port 8081! ");
		log.info("\n Enjoy exploring your API! ");
	}

}
