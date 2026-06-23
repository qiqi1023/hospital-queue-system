package com.hospital.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableCaching
@EntityScan("com.hospital.queue.model")
public class HospitalQueueSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalQueueSystemApplication.class, args);
	}

}
