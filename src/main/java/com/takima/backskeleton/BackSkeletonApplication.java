package com.takima.backskeleton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Random;
import java.util.random.RandomGenerator;

@SpringBootApplication
public class BackSkeletonApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackSkeletonApplication.class, args);
	}

	// Injected wherever we draw at random, so tests can use a fixed seed. java.util.Random: in java.base, so in any JRE
	// (getDefault() needs the jdk.random module, missing from the Docker JRE), and thread-safe for this shared instance.
	@Bean
	RandomGenerator randomGenerator() {
		return new Random();
	}

}
