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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QueueControllerTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MutableQueueClock queueClock;

	@TestConfiguration
	static class FixedClockConfig {
		@Bean
		@Primary
		MutableQueueClock fixedQueueClock() {
			return new MutableQueueClock(LocalTime.of(9, 0));
		}
	}

	static class MutableQueueClock extends Clock {
		private Instant instant;

		MutableQueueClock(LocalTime time) {
			setTime(time);
		}

		void setTime(LocalTime time) {
			this.instant = ZonedDateTime.of(LocalDate.of(2026, 5, 30), time, QueueRules.MALAYSIA_ZONE).toInstant();
		}

		@Override
		public ZoneId getZone() {
			return QueueRules.MALAYSIA_ZONE;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return Clock.fixed(instant, zone);
		}

		@Override
		public Instant instant() {
			return instant;
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
						"phoneNumber": "011-23456789",
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
						"phoneNumber": "012-3456789",
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
						"phoneNumber": "014-1112222",
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
	void callNextReturnsPriorityTicketBeforeNormalTicket() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Lim Wei Han",
						"icNumber": "060708-06-3333",
						"phoneNumber": "016-3334444",
						"departmentCode": "LAB",
						"visitReason": "Blood test",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("LAB001")))
			.andExpect(jsonPath("$.peopleAhead", is(0)));

		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Mariam binti Hassan",
						"icNumber": "070809-07-4444",
						"phoneNumber": "017-4445555",
						"departmentCode": "LAB",
						"visitReason": "Blood test",
						"priorityCategory": "PRIORITY"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("LAB002")))
			.andExpect(jsonPath("$.peopleAhead", is(0)));

		mockMvc.perform(get("/api/queues/LAB001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.peopleAhead", is(1)));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "LAB",
						"counterName": "Counter 2"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("LAB002")))
			.andExpect(jsonPath("$.priorityCategory", is("PRIORITY")))
			.andExpect(jsonPath("$.status", is("CALLED")));
	}

	@Test
	void updateStatusMarksCalledTicketAsCompleted() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Siti Aminah",
						"icNumber": "050607-05-2222",
						"phoneNumber": "015-3334444",
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
	void currentServicesShowsCounterAndActiveTicket() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Nur Iman",
						"icNumber": "080910-08-5555",
						"phoneNumber": "018-5556666",
						"departmentCode": "GEN",
						"visitReason": "Consultation",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/queues/current-services"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].counterName", is("Counter 1")))
			.andExpect(jsonPath("$[0].queueNumber", is("GEN001")))
			.andExpect(jsonPath("$[0].status", is("CALLED")));
	}

	@Test
	void duplicateActiveIcInSameDepartmentIsRejected() throws Exception {
		String requestBody = """
			{
				"patientName": "Ravi Kumar",
				"icNumber": "030405-03-9999",
				"phoneNumber": "013-2223333",
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

	@Test
	void dailyQuotaCountsCancelledAndMissedTicketsBecauseNumbersAreNotReused() throws Exception {
		takeQueue("DEN", "010101010001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("DEN001")));
		updateTicketStatus("DEN001", "CANCELLED")
			.andExpect(status().isOk());

		takeQueue("DEN", "010101010002", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("DEN002")));
		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "DEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("DEN002")));
		updateTicketStatus("DEN002", "MISSED")
			.andExpect(status().isOk());

		for (int index = 3; index <= 40; index++) {
			takeQueue("DEN", "01010101%04d".formatted(index), "NORMAL")
				.andExpect(status().isCreated());
		}

		takeQueue("DEN", "010101010041", "NORMAL")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", containsString("Daily quota for Dental Clinic is full")));
	}

	@Test
	void invalidStatusTransitionsAreRejected() throws Exception {
		takeQueue("GEN", "020202020001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN001")));

		updateTicketStatus("GEN001", "CANCELLED")
			.andExpect(status().isOk());

		updateTicketStatus("GEN001", "SERVING")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Ticket status cannot be changed from CANCELLED to SERVING.")));

		takeQueue("GEN", "020202020002", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN002")));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("GEN002")));

		updateTicketStatus("GEN002", "COMPLETED")
			.andExpect(status().isOk());

		updateTicketStatus("GEN002", "WAITING")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Ticket status cannot be changed from COMPLETED to WAITING.")));
	}

	@Test
	void takeQueueAfterCloseTimeIsRejected() throws Exception {
		queueClock.setTime(LocalTime.of(23, 59));

		takeQueue("GEN", "030303030001", "NORMAL")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Online queue closes daily at 10:00 PM.")));
	}

	@Test
	void invalidMalaysianMobileNumberIsRejected() throws Exception {
		mockMvc.perform(post("/api/queues")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"patientName": "Invalid Phone",
						"icNumber": "040404040001",
						"phoneNumber": "hello-phone",
						"departmentCode": "GEN",
						"visitReason": "General check",
						"priorityCategory": "NORMAL"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.validationErrors.phoneNumber", containsString("valid Malaysian mobile number")));
	}

	@Test
	void ticketResponseIncludesEstimatedWaitTimeFromDepartmentAverageServiceDuration() throws Exception {
		takeQueue("GEN", "050505050001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN001")))
			.andExpect(jsonPath("$.estimatedWaitMinutes", is(0)));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk());

		queueClock.setTime(LocalTime.of(9, 12));
		updateTicketStatus("GEN001", "COMPLETED")
			.andExpect(status().isOk());

		takeQueue("GEN", "050505050002", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.peopleAhead", is(0)))
			.andExpect(jsonPath("$.estimatedWaitMinutes", is(0)));

		takeQueue("GEN", "050505050003", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.peopleAhead", is(1)))
			.andExpect(jsonPath("$.estimatedWaitMinutes", is(12)));
	}

	@Test
	void estimatedWaitUsesFifteenMinutesPerPatientBeforeServiceHistoryExists() throws Exception {
		takeQueue("PHA", "080808080001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("PHA001")))
			.andExpect(jsonPath("$.peopleAhead", is(0)))
			.andExpect(jsonPath("$.estimatedWaitMinutes", is(0)));

		takeQueue("PHA", "080808080002", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("PHA002")))
			.andExpect(jsonPath("$.peopleAhead", is(1)))
			.andExpect(jsonPath("$.estimatedWaitMinutes", is(15)));
	}

	@Test
	void patientCanCancelWaitingTicket() throws Exception {
		takeQueue("LAB", "060606060001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("LAB001")));

		mockMvc.perform(put("/api/queues/LAB001/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CANCELLED")));

		mockMvc.perform(get("/api/queues")
				.param("department", "LAB")
				.param("status", "WAITING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()", is(0)));
	}

	@Test
	void staffCanListWaitingTicketsByDepartmentInQueueOrder() throws Exception {
		takeQueue("SPC", "070707070001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("SPC001")));

		takeQueue("SPC", "070707070002", "PRIORITY")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("SPC002")));

		mockMvc.perform(get("/api/queues")
				.param("department", "SPC")
				.param("status", "WAITING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()", is(2)))
			.andExpect(jsonPath("$[0].queueNumber", is("SPC002")))
			.andExpect(jsonPath("$[0].priorityCategory", is("PRIORITY")))
			.andExpect(jsonPath("$[1].queueNumber", is("SPC001")));
	}

	@Test
	void counterCannotCallAnotherTicketWhileItIsBusy() throws Exception {
		takeQueue("GEN", "090909090001", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN001")));
		takeQueue("GEN", "090909090002", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("GEN002")));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("GEN001")));

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Counter 1 is already calling or serving ticket GEN001.")));

		updateTicketStatus("GEN001", "COMPLETED")
			.andExpect(status().isOk());

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 1"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.queueNumber", is("GEN002")));
	}

	@Test
	void waitingTicketCannotBeMarkedMissedBeforeItIsCalled() throws Exception {
		takeQueue("LAB", "090909090003", "NORMAL")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.queueNumber", is("LAB001")));

		updateTicketStatus("LAB001", "MISSED")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("Ticket status cannot be changed from WAITING to MISSED.")));
	}

	@Test
	void currentQueueSummaryShowsCounterAndServiceStatus() throws Exception {
		takeQueue("GEN", "090909090004", "NORMAL")
			.andExpect(status().isCreated());

		mockMvc.perform(put("/api/queues/next-call")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"departmentCode": "GEN",
						"counterName": "Counter 3"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/departments/current-queues"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].departmentCode", is("GEN")))
			.andExpect(jsonPath("$[0].currentQueueNumber", is("GEN001")))
			.andExpect(jsonPath("$[0].counterName", is("Counter 3")))
			.andExpect(jsonPath("$[0].serviceStatus", is("CALLED")));
	}

	private ResultActions takeQueue(String departmentCode, String icNumber, String priorityCategory) throws Exception {
		return mockMvc.perform(post("/api/queues")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"patientName": "Test Patient %s",
					"icNumber": "%s",
					"phoneNumber": "012-3456789",
					"departmentCode": "%s",
					"visitReason": "General check",
					"priorityCategory": "%s"
				}
				""".formatted(icNumber, icNumber, departmentCode, priorityCategory)));
	}

	private ResultActions updateTicketStatus(String queueNumber, String status) throws Exception {
		return mockMvc.perform(put("/api/queues/%s/status".formatted(queueNumber))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
					"status": "%s"
				}
				""".formatted(status)));
	}
}
