package co.edu.uniajc.rsu_auth_service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.AuditEntryRepository;
import co.edu.uniajc.rsu_auth_service.repository.RoleRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SpaceBlockIntegrationTests {
    private static final String ORION_SPACE_ID = "20000000-0000-0000-0000-000000000001";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuditEntryRepository auditEntryRepository;

    @Test
    void activeBlockPreventsReservationsAndCancellationReleasesThePeriod() throws Exception {
        String adminToken = createAdministratorToken();
        Map<String, Object> block = Map.of(
                "spaceId", ORION_SPACE_ID,
                "startAt", "2032-06-05T09:00:00Z",
                "endAt", "2032-06-05T11:00:00Z",
                "reason", "Scheduled maintenance");

        String response = mockMvc.perform(post("/api/v1/space-blocks")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(block)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String blockId = objectMapper.readTree(response).path("id").asText();

        String userToken = registerUserToken();
        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"spaceId\":\"" + ORION_SPACE_ID
                        + "\",\"startAt\":\"2032-06-05T09:30:00Z\",\"endAt\":\"2032-06-05T10:30:00Z\",\"purpose\":\"Blocked slot\",\"attendees\":4}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/space-blocks/{blockId}/cancel", blockId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/reservations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"spaceId\":\"" + ORION_SPACE_ID
                        + "\",\"startAt\":\"2032-06-05T09:30:00Z\",\"endAt\":\"2032-06-05T10:30:00Z\",\"purpose\":\"Released slot\",\"attendees\":4}"))
                .andExpect(status().isCreated());

        org.junit.jupiter.api.Assertions.assertTrue(auditEntryRepository.count() >= 3);
    }

    private String createAdministratorToken() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.edu";
        Role administrator = roleRepository.findByName(RoleName.ADMINISTRATOR).orElseThrow();
        userRepository.save(new User("Admin User", email, passwordEncoder.encode("A-long-local-password-2026"), administrator));
        return login(email);
    }

    private String registerUserToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Regular User",
                        "email", "user-" + UUID.randomUUID() + "@example.edu",
                        "password", "A-long-local-password-2026"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"A-long-local-password-2026\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
