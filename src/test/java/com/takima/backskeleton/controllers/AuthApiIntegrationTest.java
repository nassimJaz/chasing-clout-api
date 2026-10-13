package com.takima.backskeleton.controllers;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The whole chain with the real security: sign up or log in over HTTP, then use the token.
// Needs the database (docker compose up -d); rolled back at the end.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiIntegrationTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void signUpLogInAndUseTheToken() throws Exception {
        String name = newName();
        String token = token("/auth/register", name, "motdepasse");

        mvc.perform(get("/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.memeBalance").value(5000));

        String again = token("/auth/login", name, "motdepasse");
        mvc.perform(get("/me").header("Authorization", "Bearer " + again))
                .andExpect(jsonPath("$.username").value(name));
        auth("/auth/login", name, "pas-le-bon").andExpect(status().isUnauthorized());
    }

    @Test
    void playerRoutesNeedAValidToken() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/me/free-booster")).andExpect(status().isUnauthorized());
        mvc.perform(post("/market/listings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/me").header("Authorization", "Bearer pas-un-jeton")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").header("Authorization", "Bearer " + forgedToken())).andExpect(status().isUnauthorized());

        // the catalog and the market stay public
        mvc.perform(get("/cards")).andExpect(status().isOk());
        mvc.perform(get("/market/listings")).andExpect(status().isOk());
    }

    @Test
    void takenNameIsAConflict() throws Exception {
        String name = newName();
        auth("/auth/register", name, "motdepasse").andExpect(status().isOk());
        auth("/auth/register", name, "autremotdepasse").andExpect(status().isConflict());
    }

    private ResultActions auth(String url, String name, String password) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + name + "\", \"password\": \"" + password + "\"}"));
    }

    private String token(String url, String name, String password) throws Exception {
        String body = auth(url, name, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String newName() {
        return "auth-" + System.nanoTime() % 1_000_000_000L; // names are 20 characters at most
    }

    // A well-formed token for player 1, signed with another key than the API's.
    private static String forgedToken() {
        var key = new SecretKeySpec("not-the-api-secret-but-32-chars-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").expiresAt(Instant.now().plusSeconds(60)).build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
