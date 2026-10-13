package com.takima.backskeleton.services;

import com.takima.backskeleton.DAO.AppUserDao;
import com.takima.backskeleton.models.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

// Sign up and log in; both answer a JWT whose subject is the player's id.
// ponytail: logging out only drops the token on the client, it stays valid until it expires; add a deny list if it matters.
@Service
public class AuthService {
    public static final long STARTING_BALANCE = 5000;
    public static final Duration TOKEN_LIFETIME = Duration.ofHours(24);

    private final AppUserDao appUserDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(AppUserDao appUserDao, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.appUserDao = appUserDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public String register(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9_.-]{3,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le pseudo doit faire de 3 à 20 caractères (lettres, chiffres, . _ -)");
        }
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit faire au moins 8 caractères");
        }
        try {
            return token(appUserDao.saveAndFlush(new AppUser(username, passwordEncoder.encode(password), STARTING_BALANCE)));
        } catch (DataIntegrityViolationException e) {
            // the unique constraint settles it, even for two simultaneous sign-ups with the same name
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce pseudo est déjà pris");
        }
    }

    // Same message for an unknown name and a wrong password.
    public String login(String username, String password) {
        return appUserDao.findByUsername(username)
                .filter(user -> password != null && passwordEncoder.matches(password, user.getPasswordHash()))
                .map(this::token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pseudo ou mot de passe incorrect"));
    }

    private String token(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
