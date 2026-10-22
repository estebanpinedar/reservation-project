package co.edu.uniajc.rsu_auth_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationIntegrationTests {
    private static final String ORION_SPACE_ID = "20000000-0000-0000-0000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void activeReservationsCannotOverlapAndCancelledReservationsFreeThePeriod() throws Exception {
        String token = registerAndGetToken();
        Map<String, Object> reservation = Map.of(
                "spaceId", ORION_SPACE_ID,
                "startAt", "2032-05-05T09:00:00Z",
                "endAt", "2032-05-05T11:00:00Z",
                "purpose", "Architecture workshop",
                "attendees", 10);

        String creation = mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reservation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String reservationId = objectMapper.readTree(creation).path("id").asText();

        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "spaceId", ORION_SPACE_ID,
                        "startAt", "2032-05-05T10:00:00Z",
                        "endAt", "2032-05-05T12:00:00Z",
                        "purpose", "Overlapping workshop",
                        "attendees", 8))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/reservations/{reservationId}/cancel", reservationId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"The event was rescheduled\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reservation)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/reservations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String registerAndGetToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Reservation User",
                        "email", "reservations-" + UUID.randomUUID() + "@example.edu",
                        "password", "A-long-local-password-2026"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
