package com.hospital.queue.config;

import com.hospital.queue.constant.QueueRules;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
	@Bean
	public Clock queueClock() {
		return Clock.system(QueueRules.MALAYSIA_ZONE);
	}
}
