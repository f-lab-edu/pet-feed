package com.petfeed.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petfeed.infrastructure.jwt.JwtTokenProvider;
import com.petfeed.user.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:member-crud-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MemberCrudIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtTokenProvider tokens;

    @BeforeEach
    void reset() {
        members.deleteAll();
    }

    @Test
    void signupLoginReadUpdateAndDeleteLifecycle() throws Exception {
        String key = signup("owner@example.com");
        var stored = members.findById(key).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("secret123!");
        assertThat(passwords.matches("secret123!", stored.getPasswordHash())).isTrue();

        JsonNode login = json.readTree(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("OWNER@example.com", "secret123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grantType").value("Bearer"))
                .andReturn().getResponse().getContentAsString());
        assertThat(tokens.validateToken(login.get("accessToken").asText())).isTrue();
        assertThat(tokens.getUserKey(login.get("accessToken").asText())).isEqualTo(key);

        mvc.perform(get("/api/v1/users/{key}", key))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("owner"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mvc.perform(put("/api/v1/users/{key}", key).header("X-User-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"new bio\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/users/{key}", key))
                .andExpect(status().isOk()).andExpect(jsonPath("$.bio").value("new bio"));
        assertThat(members.findById(key).orElseThrow().getEmail()).isEqualTo("new@example.com");
        loginStatus("owner@example.com", "secret123!", 401);
        loginStatus("new@example.com", "secret123!", 200);

        mvc.perform(delete("/api/v1/users/{key}", key).header("X-User-Key", key))
                .andExpect(status().isNoContent());
        assertThat(members.findById(key)).isEmpty();
        mvc.perform(get("/api/v1/users/{key}", key)).andExpect(status().isNotFound());
        loginStatus("new@example.com", "secret123!", 401);
        mvc.perform(delete("/api/v1/users/{key}", key).header("X-User-Key", key))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateSignupAndEmailUpdateAreRejected() throws Exception {
        String owner = signup("owner@example.com");
        String other = signup("other@example.com");
        mvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("OWNER@example.com")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/v1/users/{key}", other).header("X-User-Key", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@example.com\",\"bio\":\"changed\"}"))
                .andExpect(status().isConflict());
        assertThat(members.findById(other).orElseThrow().getBio()).isNull();
        mvc.perform(put("/api/v1/users/{key}", owner).header("X-User-Key", owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"owner@example.com\"}"))
                .andExpect(status().isNoContent());
        assertThat(members.count()).isEqualTo(2);
    }

    @Test
    void invalidInputsAreRejectedWithoutWriting() throws Exception {
        mvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("invalid"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"valid@example.com\",\"password\":\"\",\"phone\":\"\",\"nickname\":\"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(members.count()).isZero();
        String key = signup("owner@example.com");
        for (String email : new String[]{"invalid", "", "   "}) {
            mvc.perform(put("/api/v1/users/{key}", key).header("X-User-Key", key)
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("email", email))))
                    .andExpect(status().isBadRequest());
        }
        assertThat(members.findById(key).orElseThrow().getEmail()).isEqualTo("owner@example.com");
    }

    @Test
    void wrongCredentialsAndUnauthorizedChangesAreRejected() throws Exception {
        String key = signup("owner@example.com");
        loginStatus("owner@example.com", "wrong", 401);
        loginStatus("missing@example.com", "secret123!", 401);
        mvc.perform(put("/api/v1/users/{key}", key).header("X-User-Key", "other")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"bio\":\"changed\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/{key}", key).header("X-User-Key", "other"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/{key}", key)).andExpect(status().isBadRequest());
        assertThat(members.findById(key).orElseThrow().getBio()).isNull();
    }

    private String signup(String email) throws Exception {
        String body = mvc.perform(post("/api/v1/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody(email))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").exists()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("userKey").asText();
    }

    private void loginStatus(String email, String password, int statusCode) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password))).andExpect(status().is(statusCode));
    }

    private String credentials(String email, String password) throws Exception {
        return json.writeValueAsString(java.util.Map.of("email", email, "password", password));
    }

    private String signupBody(String email) throws Exception {
        return json.writeValueAsString(java.util.Map.of("email", email, "password", "secret123!", "phone", "01012345678", "nickname", "owner"));
    }
}
