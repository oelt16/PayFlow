package com.payflow.merchant.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers(disabledWithoutDocker = true)
class MerchantApiIntegrationTest extends MerchantIntegrationInfrastructure {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postMerchantsReturns201WithApiKey() throws Exception {
        mockMvc.perform(
                        post("/v1/merchants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Acme\",\"email\":\"acme@example.com\"}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.apiKey").value(startsWith("sk_test_")))
                .andExpect(jsonPath("$.email").value("acme@example.com"))
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void getMeWithoutAuthReturns401() throws Exception {
        mockMvc.perform(get("/v1/merchants/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("invalid_api_key"));
    }

    @Test
    void getMeWithValidKeyReturnsMerchant() throws Exception {
        String key = registerAndExtractKey("Shop", "shop@example.com");

        mockMvc.perform(
                        get("/v1/merchants/me")
                                .header("Authorization", "Bearer " + key)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("shop@example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        mockMvc.perform(
                        post("/v1/merchants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"A\",\"email\":\"dup@example.com\"}")
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/v1/merchants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"B\",\"email\":\"dup@example.com\"}")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("duplicate_email"));
    }

    @Test
    void deleteMeDeactivatesMerchant() throws Exception {
        String key = registerAndExtractKey("Del", "del@example.com");

        mockMvc.perform(
                        delete("/v1/merchants/me")
                                .header("Authorization", "Bearer " + key)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/v1/merchants/me")
                                .header("Authorization", "Bearer " + key)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotateApiKeyInvalidatesOldKey() throws Exception {
        String oldKey = registerAndExtractKey("Rot", "rot@example.com");

        MvcResult rotated = mockMvc.perform(
                        post("/v1/merchants/me/api-keys")
                                .header("Authorization", "Bearer " + oldKey)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").exists())
                .andReturn();

        String newKey = objectMapper.readTree(rotated.getResponse().getContentAsString()).get("apiKey").asText();

        mockMvc.perform(
                        get("/v1/merchants/me")
                                .header("Authorization", "Bearer " + oldKey)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        get("/v1/merchants/me")
                                .header("Authorization", "Bearer " + newKey)
                )
                .andExpect(status().isOk());
    }

    private String registerAndExtractKey(String name, String email) throws Exception {
        MvcResult r = mockMvc.perform(
                        post("/v1/merchants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\"}")
                )
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(r.getResponse().getContentAsString());
        assertThat(root.has("apiKey")).isTrue();
        return root.get("apiKey").asText();
    }
}
