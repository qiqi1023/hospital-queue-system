package com.hospital.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class HospitalQueueSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(HospitalQueueSystemApplication.class, args);
	}

}
