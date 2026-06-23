package com.hospital.queue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "phone_codes")
public class PhoneCode {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private String countryName;
	@Column(nullable = false, unique = true, length = 2)
	private String isoCode;
	@Column(nullable = false, length = 10)
	private String dialCode;
	protected PhoneCode() {}
	public Long getId() { return id; }
	public String getCountryName() { return countryName; }
	public String getIsoCode() { return isoCode; }
	public String getDialCode() { return dialCode; }
}
