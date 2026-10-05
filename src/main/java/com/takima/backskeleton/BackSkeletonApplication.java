package com.takima.backskeleton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.random.RandomGenerator;

@SpringBootApplication
public class BackSkeletonApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackSkeletonApplication.class, args);
	}

	// Injected wherever we draw at random, so tests can use a fixed seed.
	@Bean
	RandomGenerator randomGenerator() {
		return RandomGenerator.getDefault();
	}

}
