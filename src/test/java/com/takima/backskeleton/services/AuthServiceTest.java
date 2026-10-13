package com.takima.backskeleton.services;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.models.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Pure unit test: no Spring, no database. The DAO is a Mockito mock (a fake whose answers each test scripts with
// when(...)); password hashing and JWT signing are the real ones. JUnit builds a new instance per test: fresh mock each time.
class AuthServiceTest {
    private static final SecretKey KEY =
            new SecretKeySpec("a-test-secret-of-at-least-32-chars".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final AppUserDao appUserDao = mock(AppUserDao.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // low cost: fast tests
    private final AuthService authService =
            new AuthService(appUserDao, passwordEncoder, new NimbusJwtEncoder(new ImmutableSecret<>(KEY)));
    private final JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(KEY).build();

    @Test
    void registerSavesAHashedPasswordAndGivesATokenForTheNewPlayer() {
        // the "database" gives the saved player id 42
        when(appUserDao.saveAndFlush(any())).thenAnswer(call -> withId(call.getArgument(0), 42L));

        String token = authService.register("alice", "motdepasse");

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserDao).saveAndFlush(saved.capture());
        assertEquals("alice", saved.getValue().getUsername());
        assertEquals(AuthService.STARTING_BALANCE, saved.getValue().getMemeBalance());
        assertNotEquals("motdepasse", saved.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches("motdepasse", saved.getValue().getPasswordHash()));

        Jwt jwt = jwtDecoder.decode(token); // throws if the signature is wrong
        assertEquals("42", jwt.getSubject());
        assertEquals("alice", jwt.getClaimAsString("username"));
        assertEquals(AuthService.TOKEN_LIFETIME, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void takenNameIsAConflict() {
        when(appUserDao.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.register("alice", "motdepasse"));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @ParameterizedTest
    @CsvSource({
            "ab, motdepasse",                       // name too short
            "un-pseudo-bien-trop-long, motdepasse", // name too long (24)
            "avec espace, motdepasse",              // forbidden character
            ", motdepasse",                         // no name (an empty CSV value is null)
            "alice, court",                         // password under 8 characters
            "alice, ",                              // no password
    })
    void invalidSignUpIsABadRequest(String username, String password) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.register(username, password));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        verifyNoInteractions(appUserDao); // rejected before reaching the database
    }

    @Test
    void loginWithTheRightPasswordGivesATokenForThatPlayer() {
        AppUser alice = withId(new AppUser("alice", passwordEncoder.encode("motdepasse"), 0), 7L);
        when(appUserDao.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertEquals("7", jwtDecoder.decode(authService.login("alice", "motdepasse")).getSubject());
    }

    @Test
    void wrongPasswordOrUnknownNameIsUnauthorized() {
        AppUser alice = withId(new AppUser("alice", passwordEncoder.encode("motdepasse"), 0), 7L);
        when(appUserDao.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(appUserDao.findByUsername("bob")).thenReturn(Optional.empty());

        ResponseStatusException wrongPassword = assertThrows(ResponseStatusException.class,
                () -> authService.login("alice", "pas-le-bon"));
        ResponseStatusException unknownName = assertThrows(ResponseStatusException.class,
                () -> authService.login("bob", "motdepasse"));
        assertEquals(HttpStatus.UNAUTHORIZED, wrongPassword.getStatusCode());
        // same answer in both cases: the error doesn't tell which one was wrong
        assertEquals(wrongPassword.getReason(), unknownName.getReason());
    }

    // The id is set by the database in real life.
    private static AppUser withId(AppUser user, long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
