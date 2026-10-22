package co.edu.uniajc.rsu_auth_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationConcurrencyIntegrationTests {
    private static final String ORION_SPACE_ID = "20000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void simultaneousRequestsForTheSameSpaceAndPeriodCreateOnlyOneReservation() throws Exception {
        String firstToken = registerAndGetToken();
        String secondToken = registerAndGetToken();
        String body = objectMapper.writeValueAsString(Map.of(
                "spaceId", ORION_SPACE_ID,
                "startAt", "2042-08-17T14:00:00Z",
                "endAt", "2042-08-17T15:00:00Z",
                "purpose", "Concurrent integration verification",
                "attendees", 4));
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> createReservationWhenReleased(startTogether, firstToken, body));
            Future<Integer> second = executor.submit(() -> createReservationWhenReleased(startTogether, secondToken, body));
            startTogether.countDown();
            int firstStatus = first.get();
            int secondStatus = second.get();
            assertEquals(1, (firstStatus == 201 ? 1 : 0) + (secondStatus == 201 ? 1 : 0));
            assertTrue(firstStatus == 409 || secondStatus == 409,
                    "The losing reservation request must receive a conflict response");
        } finally {
            executor.shutdownNow();
        }
    }

    private int createReservationWhenReleased(CountDownLatch latch, String token, String body) throws Exception {
        latch.await();
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        return result.getResponse().getStatus();
    }

    private String registerAndGetToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "name", "Concurrent User",
                        "email", "concurrent-" + UUID.randomUUID() + "@example.edu",
                        "password", "A-long-local-password-2026"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
