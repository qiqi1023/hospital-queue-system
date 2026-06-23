package com.hospital.queue;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:queue_${random.uuid}")
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class QueueControllerTests {
	@Autowired MockMvc mvc;

	@TestConfiguration static class ClockConfig {
		@Bean @Primary Clock clock() {
			return Clock.fixed(ZonedDateTime.of(2026, 6, 24, 9, 0, 0, 0, ZoneId.of("Asia/Kuala_Lumpur")).toInstant(), ZoneId.of("Asia/Kuala_Lumpur"));
		}
	}

	@Test void createsMalaysianTicketAndNormalizesPhone() throws Exception {
		create(validMalaysian()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.message", is("Queue number successfully generated")))
			.andExpect(jsonPath("$.data.queueNumber", is("GEN001")))
			.andExpect(jsonPath("$.data.identityNumber").doesNotExist());
	}

	@Test void createsNonMalaysianTicket() throws Exception {
		create("""
			{"identityType":"NON_MALAYSIAN","identityNumber":"E3905107K",
			 "phoneCountryCode":"+65","phoneNumber":"81234567","departmentCode":"GEN"}
			""").andExpect(status().isCreated()).andExpect(jsonPath("$.data.status", is("WAITING")));
	}

	@Test void rejectsInvalidBirthDateAndState() throws Exception {
		create(validMalaysian().replace("900101101234", "910229101234"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", is("Please enter a valid identity or passport number.")));
		create(validMalaysian().replace("900101101234", "900101991234"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", is("Please enter a valid identity or passport number.")));
	}

	@Test void preventsDuplicateActiveTicket() throws Exception {
		create(validMalaysian()).andExpect(status().isCreated());
		create(validMalaysian()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.message", is("You already have an active queue ticket for this department today.")));
	}

	@Test void callsNextAndUpdatesStatus() throws Exception {
		create(validMalaysian()).andExpect(status().isCreated());
		mvc.perform(post("/api/queueCalls").contentType(MediaType.APPLICATION_JSON)
			.content("{\"departmentCode\":\"GEN\",\"counterName\":\"Counter 1\"}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.data.status", is("CALLED")));
		mvc.perform(patch("/api/queueTickets/GEN001/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"COMPLETED\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.completedAt", notNullValue()));
	}

	@Test void missedTicketCanReturnBehindPatientsAlreadyWaiting() throws Exception {
		create(validMalaysian()).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.queueNumber", is("GEN001")));
		create(validMalaysian().replace("900101101234", "900101101235"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.queueNumber", is("GEN002")));

		mvc.perform(post("/api/queueCalls").contentType(MediaType.APPLICATION_JSON)
			.content("{\"departmentCode\":\"GEN\",\"counterName\":\"Counter 1\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.queueNumber", is("GEN001")));
		mvc.perform(patch("/api/queueTickets/GEN001/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"MISSED\"}"))
			.andExpect(status().isOk());
		mvc.perform(patch("/api/queueTickets/GEN001/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"WAITING\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.peopleAhead", is(1)))
			.andExpect(jsonPath("$.data.counterName").value(nullValue()));

		mvc.perform(post("/api/queueCalls").contentType(MediaType.APPLICATION_JSON)
			.content("{\"departmentCode\":\"GEN\",\"counterName\":\"Counter 1\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.queueNumber", is("GEN002")));
	}

	@Test void supportsFilteringAndPagination() throws Exception {
		create(validMalaysian()).andExpect(status().isCreated());
		mvc.perform(get("/api/queueTickets").param("departmentCode","GEN").param("status","WAITING")
			.param("queueDate","2026-06-24").param("page","0").param("size","10").param("sort","createdAt,asc"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data.content", hasSize(1)));
	}

	@Test void returnsReferenceDataAndCurrentQueue() throws Exception {
		mvc.perform(get("/api/departments")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(5)));
		mvc.perform(get("/api/counters")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(6)));
		mvc.perform(get("/api/phoneCodes")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(19)));
		mvc.perform(get("/api/icStates")).andExpect(status().isOk()).andExpect(jsonPath("$.data", not(empty())));
		mvc.perform(get("/api/queues/current")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(5)));
	}

	@Test void rendersPatientJspWithDatabaseReferenceData() throws Exception {
		mvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("index"))
			.andExpect(model().attribute("departments", hasSize(5)))
			.andExpect(model().attribute("phoneCodes", hasSize(19)));
	}

	@Test void rendersStaffJspWithDatabaseDepartments() throws Exception {
		mvc.perform(get("/staff"))
			.andExpect(status().isOk())
			.andExpect(view().name("staff"))
			.andExpect(model().attribute("departments", hasSize(5)));
	}

	private org.springframework.test.web.servlet.ResultActions create(String body) throws Exception {
		return mvc.perform(post("/api/queueTickets").contentType(MediaType.APPLICATION_JSON).content(body));
	}
	private String validMalaysian() { return """
		{"identityType":"MALAYSIAN","identityNumber":"900101101234",
		 "phoneCountryCode":"+60","phoneNumber":"1123456789","departmentCode":"GEN"}
		"""; }
}
