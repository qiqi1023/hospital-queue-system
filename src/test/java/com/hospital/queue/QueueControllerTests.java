package com.hospital.queue;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hospital.queue.constant.QueueRules;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class QueueControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		@Primary
		Clock fixedQueueClock() {
			return Clock.fixed(
				ZonedDateTime.of(
					LocalDate.of(2026, 5, 30),
					LocalTime.of(9, 0),
					QueueRules.MALAYSIA_ZONE
				).toInstant(),
				QueueRules.MALAYSIA_ZONE
			);
		}
	}

	@Test
	void takeQueueReturnsGeneratedQueueNumber() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Nur Aisyah binti Ahmad",
						"icNumber": "010203-01-1234",
						"phoneNumber": "011-2345 6789",
						"departmentCode": "GEN",
						"visitReason": "Fever and cough",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN001")))
			.andExpect(jsonPath("$.departmentName", is("General Consultation")))
			.andExpect(jsonPath("$.status", is("WAITING")))
			.andExpect(jsonPath("$.peopleAhead", is(0)));
	}

	@Test
	void checkTicketReturnsExistingTicket() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Tan Mei Ling",
						"icNumber": "020304-02-5678",
						"phoneNumber": "012-345 6789",
						"departmentCode": "PHA",
						"visitReason": "Collect medicine",
						"priorityCategory": "PRIORITY"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("PHA001")));

		mockMvc.perform(get("/api/queues/PHA001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.patientName", is("Tan Mei Ling")))
			.andExpect(jsonPath("$.priorityCategory", is("PRIORITY")));
	}

	@Test
	void callNextReturnsEarliestWaitingTicketAndAssignsCounter() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Ahmad Firdaus",
						"icNumber": "040506-04-1111",
						"phoneNumber": "014-111 2222",
						"departmentCode": "LAB",
						"visitReason": "Blood test",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("LAB001")));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "LAB",
						"counterName": "Counter 2"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("LAB001")))
			.andExpect(jsonPath("$.status", is("CALLED")))
			.andExpect(jsonPath("$.counterName", is("Counter 2")))
			.andExpect(jsonPath("$.calledAt", is("2026-05-30T09:00:00")));
	}

	@Test
	void updateStatusMarksCalledTicketAsCompleted() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Siti Aminah",
						"icNumber": "050607-05-2222",
						"phoneNumber": "015-333 4444",
						"departmentCode": "SPC",
						"visitReason": "Specialist follow-up",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("SPC001")));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "SPC",
						"counterName": "Counter 4"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CALLED")));

		mockMvc.perform(put("/api/queues/SPC001/status")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"status": "COMPLETED"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("COMPLETED")))
			.andExpect(jsonPath("$.completedAt", is("2026-05-30T09:00:00")));
	}

	@Test
	void duplicateActiveIcInSameDepartmentIsRejected() throws Exception {
		String requestBody = """
			{
				"patientName": "Ravi Kumar",
				"icNumber": "030405-03-9999",
				"phoneNumber": "013-222 3333",
				"departmentCode": "DEN",
				"visitReason": "Tooth pain",
				"priorityCategory": "NORMAL"
			}
			""";

		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", containsString("already has an active ticket")));
	}
}
