package co.edu.uniajc.rsu_auth_service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.RoleRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminManagementIntegrationTests {
    private static final String ORION_SPACE_ID = "20000000-0000-0000-0000-000000000001";
    private static final String AUDITORIUM_SPACE_ID = "20000000-0000-0000-0000-000000000002";
    private static final String PASSWORD = "A-long-local-password-2026";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void administratorCanDeactivateAnAccountAndItsExistingTokenStopsWorking() throws Exception {
        String administratorToken = createUserToken(RoleName.ADMINISTRATOR);
        String userToken = createUserToken(RoleName.USER);
        String userId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("id").asText();

        mockMvc.perform(patch("/api/v1/users/{userId}/status", userId)
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administratorCannotDeactivateItsOwnAccount() throws Exception {
        String administratorToken = createUserToken(RoleName.ADMINISTRATOR);
        String administratorId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                .header("Authorization", "Bearer " + administratorToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("id").asText();

        mockMvc.perform(patch("/api/v1/users/{userId}/status", administratorId)
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/users/{userId}/roles", administratorId)
                .header("Authorization", "Bearer " + administratorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"USER\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanChangeOnlyAssignedSpaceStatusAndCanListInactiveAssignedSpaces() throws Exception {
        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();
        Space assignedSpace = spaceRepository.findById(UUID.fromString(ORION_SPACE_ID)).orElseThrow();
        User manager = new User("Assigned Manager", "manager-" + UUID.randomUUID() + "@example.edu",
                passwordEncoder.encode(PASSWORD), managerRole);
        manager.replaceManagedSpaces(Set.of(assignedSpace));
        userRepository.saveAndFlush(manager);
        String managerToken = login(manager.getEmail());

        mockMvc.perform(patch("/api/v1/spaces/{spaceId}/status", ORION_SPACE_ID)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/spaces/manage")
                .header("Authorization", "Bearer " + managerToken)
                .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORION_SPACE_ID));

        mockMvc.perform(patch("/api/v1/spaces/{spaceId}/status", AUDITORIUM_SPACE_ID)
                .header("Authorization", "Bearer " + managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.existsByIdAndManagedSpaces_Id(manager.getId(), assignedSpace.getId()));
    }

    private String createUserToken(RoleName roleName) throws Exception {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        String email = roleName.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.edu";
        userRepository.saveAndFlush(new User("Test " + roleName.name(), email,
                passwordEncoder.encode(PASSWORD), role));
        return login(email);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("accessToken").asText();
    }
}
