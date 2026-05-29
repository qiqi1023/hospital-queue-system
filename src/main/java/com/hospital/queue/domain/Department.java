package com.hospital.queue.domain;

import com.hospital.queue.constant.ResponseMessages;
import java.util.Arrays;

public enum Department {
	GEN("General Consultation", 100),
	PHA("Pharmacy", 150),
	DEN("Dental Clinic", 40),
	LAB("Blood Test Unit", 80),
	SPC("Specialist Clinic", 50);

	private final String displayName;
	private final int dailyQuota;

	Department(String displayName, int dailyQuota) {
		this.displayName = displayName;
		this.dailyQuota = dailyQuota;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getDailyQuota() {
		return dailyQuota;
	}

	public static Department fromCode(String code) {
		return Arrays.stream(values())
			.filter(department -> department.name().equalsIgnoreCase(code))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
				ResponseMessages.UNKNOWN_DEPARTMENT_CODE.formatted(code)
			));
	}
}
