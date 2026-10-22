package co.edu.uniajc.rsu_auth_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicCatalogCanBeFilteredByTextTypeAndCapacity() throws Exception {
        mockMvc.perform(get("/api/v1/space-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(get("/api/v1/spaces").param("query", "biblioteca central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sala Multimedial - Biblioteca Jairo Panesso Tascón"));

        mockMvc.perform(get("/api/v1/spaces")
                .param("type", "Laboratorio de cómputo")
                .param("minimumCapacity", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type.name").value("Laboratorio De Cómputo"));
    }
}
