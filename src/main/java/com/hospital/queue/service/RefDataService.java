package com.hospital.queue.service;

import com.hospital.queue.exception.NotFoundException;
import com.hospital.queue.model.*;
import com.hospital.queue.repository.*;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class RefDataService {
	private final DepartmentRepo departments; private final CounterRepo counters;
	private final PhoneCodeRepo phoneCodes; private final IcStateRepo icStates;
	public RefDataService(DepartmentRepo departments, CounterRepo counters, PhoneCodeRepo phoneCodes, IcStateRepo icStates) {
		this.departments=departments; this.counters=counters; this.phoneCodes=phoneCodes; this.icStates=icStates;
	}
	@Cacheable("departments") public List<Department> departments() { return departments.findAll(Sort.by("code")); }
	@Cacheable(value="department", key="#code.toUpperCase()") public Department department(String code) { return departments.findById(code.toUpperCase())
		.orElseThrow(() -> new NotFoundException("Department not found")); }
	public List<Counter> counters() { return counters.findAll(Sort.by("name")); }
	public List<Counter> counters(String code) { department(code); return counters.findByDepartmentCodeIgnoreCaseOrderByName(code); }
	@Cacheable("phoneCodes") public List<PhoneCode> phoneCodes() { return phoneCodes.findAll(Sort.by("countryName")); }
	@Cacheable("icStates") public List<IcState> icStates() { return icStates.findAll(Sort.by("code")); }
}
