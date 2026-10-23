package com.connectpro;

import com.connectpro.auth.entity.UserCredential;
import com.connectpro.auth.entity.UserSession;
import com.connectpro.auth.model.CredentialType;
import com.connectpro.auth.repository.RateLimitEntryRepository;
import com.connectpro.auth.repository.UserCredentialRepository;
import com.connectpro.auth.repository.UserSessionRepository;
import com.connectpro.auth.service.PinHashService;
import com.connectpro.user.entity.User;
import com.connectpro.user.model.UserStatus;
import com.connectpro.user.repository.UserRepository;
import com.connectpro.auth.model.VerificationTokenType;
import com.connectpro.auth.service.VerificationTokenService;
import com.connectpro.auth.repository.VerificationTokenRepository;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConnectproBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PinHashService pinHashService;

    @Autowired
    private RateLimitEntryRepository rateLimitEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @BeforeEach
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void cleanDatabase() {
        userSessionRepository.deleteAll();
        userCredentialRepository.deleteAll();
        verificationTokenRepository.deleteAll();
        rateLimitEntryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void registerCreatesPendingUserAndPinCredential() throws Exception {

        String request = """
                {
                    "phoneNumber": "+919876543211",
                    "email": "register.test@example.com",
                    "pin": "TestPin123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated());

        User user = userRepository.findByPhoneNumber("+919876543211")
                .orElseThrow();

        assertThat(user.getEmail())
                .isEqualTo("register.test@example.com");

        assertThat(user.getStatus())
                .isEqualTo(UserStatus.PENDING_VERIFICATION);

        assertThat(user.getPhoneVerifiedAt())
                .isNull();

        assertThat(user.getEmailVerifiedAt())
                .isNull();

        UserCredential credential =
                userCredentialRepository
                        .findByUserIdAndCredentialType(
                                user.getId(),
                                CredentialType.PIN
                        )
                        .orElseThrow();

        assertThat(credential.getCredentialHash())
                .isNotBlank();

        assertThat(credential.getCredentialHash())
                .doesNotContain("TestPin123");

        assertThat(userSessionRepository.findAllByUserId(user.getId()))
                .isEmpty();
    }

    @Test
    @Transactional
    void loginReturnsAccessAndRefreshTokensForVerifiedUser() throws Exception {

        User user = new User(
                "+919876543212",
                "login.test@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String request = """
            {
                "phoneNumber": "+919876543212",
                "pin": "TestPin123"
            }
            """;

        String response = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response)
                .contains("\"accessToken\"")
                .contains("\"refreshToken\"")
                .contains("\"tokenType\":\"Bearer\"")
                .contains("\"expiresIn\":900");

        assertThat(userSessionRepository.findAllByUserId(user.getId()))
                .hasSize(1);
    }

    @Test
    @Transactional
    void loginRejectsUnverifiedUser() throws Exception {

        User user = new User(
                "+919876543213",
                "unverified.login@example.com"
        );

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String request = """
            {
                "phoneNumber": "+919876543213",
                "pin": "TestPin123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized());

        assertThat(userSessionRepository.findAllByUserId(user.getId()))
                .isEmpty();
    }

    @Test
    void refreshRotatesRefreshTokenAndRejectsOldToken() throws Exception {

        User user = new User(
                "+919876543214",
                "refresh.test@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String loginRequest = """
        {
            "phoneNumber": "+919876543214",
            "pin": "TestPin123"
        }
        """;

        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String originalRefreshToken =
                objectMapper
                        .readTree(loginResponse)
                        .get("refreshToken")
                        .asText();

        String refreshRequest = """
        {
            "refreshToken": "%s"
        }
        """.formatted(originalRefreshToken);

        String refreshResponse = mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newRefreshToken =
                objectMapper
                        .readTree(refreshResponse)
                        .get("refreshToken")
                        .asText();

        assertThat(newRefreshToken)
                .isNotEqualTo(originalRefreshToken);

        assertThat(userSessionRepository.findAllByUserId(user.getId()))
                .hasSize(2);

        UserSession originalSession = userSessionRepository
                .findAllByUserId(user.getId())
                .stream()
                .filter(session -> session.getRefreshTokenSelector().equals(
                        originalRefreshToken.split("\\.", -1)[0]
                ))
                .findFirst()
                .orElseThrow();

        assertThat(originalSession.getLastUsedAt()).isNotNull();

        String oldTokenRequest = """
        {
            "refreshToken": "%s"
        }
        """.formatted(originalRefreshToken);

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(oldTokenRequest)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pinResetRevokesExistingSessions() throws Exception {

        User user = new User(
                "+919876543215",
                "pinreset.test@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        // Create an authenticated session.
        String loginRequest = """
        {
            "phoneNumber": "+919876543215",
            "pin": "TestPin123"
        }
        """;

        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken =
                objectMapper
                        .readTree(loginResponse)
                        .get("refreshToken")
                        .asText();

        assertThat(userSessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getId()))
                .hasSize(1);

        // Create a valid PIN-reset verification token.
        String verificationCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PIN_RESET
                );

        String resetRequest = """
        {
            "identifier": "+919876543215",
            "verificationCode": "%s",
            "newPin": "NewPin123"
        }
        """.formatted(verificationCode);

        mockMvc.perform(
                        post("/api/v1/auth/pin-reset")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(resetRequest)
                )
                .andExpect(status().isNoContent());

        // PIN reset must revoke all existing sessions.
        assertThat(userSessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getId()))
                .isEmpty();

        // The old refresh token must no longer be usable.
        String oldTokenRequest = """
        {
            "refreshToken": "%s"
        }
        """.formatted(refreshToken);

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(oldTokenRequest)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedLogoutRejectsUnauthenticatedRequest() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesAuthenticatedSession() throws Exception {

        User user = new User(
                "+919876543218",
                "logout.test@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String loginRequest = """
            {
                "phoneNumber": "+919876543218",
                "pin": "TestPin123"
            }
            """;

        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper
                .readTree(loginResponse)
                .get("accessToken")
                .asText();

        String refreshToken = objectMapper
                .readTree(loginResponse)
                .get("refreshToken")
                .asText();

        assertThat(userSessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getId()))
                .hasSize(1);

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isNoContent());

        assertThat(userSessionRepository.findAllByUserIdAndRevokedAtIsNull(user.getId()))
                .isEmpty();

        String refreshRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(refreshRequest)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessTokenIsAcceptedByProtectedEndpoint() throws Exception {

        User user = new User(
                "+919876543219",
                "jwt.valid.test@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String loginRequest = """
            {
                "phoneNumber": "+919876543219",
                "pin": "TestPin123"
            }
            """;

        String loginResponse = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper
                .readTree(loginResponse)
                .get("accessToken")
                .asText();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer " + accessToken)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void malformedAccessTokenIsRejected() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", "Bearer invalid.token.value")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingBearerTokenIsRejected() throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", "invalid-token")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIsRateLimitedPerPhoneNumber() throws Exception {

        String phone = "+919876543220";

        User user = new User(
                phone,
                "ratelimit.login@example.com"
        );

        user.markPhoneVerified();
        user.markEmailVerified();

        userRepository.save(user);

        UserCredential credential = new UserCredential(
                user,
                CredentialType.PIN,
                pinHashService.hash("TestPin123")
        );

        userCredentialRepository.save(credential);

        String request = """
            {
                "phoneNumber": "%s",
                "pin": "WrongPin123"
            }
            """.formatted(phone);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void phoneVerificationRejectsInvalidCode() throws Exception {

        User user = new User(
                "+919876543222",
                "verification.invalid@example.com"
        );

        userRepository.save(user);

        verificationTokenService.createToken(
                user,
                VerificationTokenType.PHONE_VERIFICATION
        );

        String request = """
            {
                "phoneNumber": "+919876543222",
                "verificationCode": "000000"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized());

        User reloaded = userRepository
                .findByPhoneNumber("+919876543222")
                .orElseThrow();

        assertThat(reloaded.getPhoneVerifiedAt())
                .isNull();
    }

    @Test
    void usedPhoneVerificationCodeCannotBeReused() throws Exception {

        User user = new User(
                "+919876543223",
                "verification.used@example.com"
        );

        userRepository.save(user);

        String verificationCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PHONE_VERIFICATION
                );

        String request = """
            {
                "phoneNumber": "+919876543223",
                "verificationCode": "%s"
            }
            """.formatted(verificationCode);

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void creatingNewPhoneVerificationCodeInvalidatesPreviousCode() throws Exception {

        User user = new User(
                "+919876543224",
                "verification.replace@example.com"
        );

        userRepository.save(user);

        String oldCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PHONE_VERIFICATION
                );

        String newCode =
                verificationTokenService.createToken(
                        user,
                        VerificationTokenType.PHONE_VERIFICATION
                );

        assertThat(newCode)
                .isNotEqualTo(oldCode);

        String oldRequest = """
            {
                "phoneNumber": "+919876543224",
                "verificationCode": "%s"
            }
            """.formatted(oldCode);

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(oldRequest)
                )
                .andExpect(status().isUnauthorized());

        String newRequest = """
            {
                "phoneNumber": "+919876543224",
                "verificationCode": "%s"
            }
            """.formatted(newCode);

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newRequest)
                )
                .andExpect(status().isOk());
    }

    @Test
    void emailVerificationRejectsInvalidCode() throws Exception {

        User user = new User(
                "+919876543225",
                "verification.email@example.com"
        );

        userRepository.save(user);

        verificationTokenService.createToken(
                user,
                VerificationTokenType.EMAIL_VERIFICATION
        );

        String request = """
            {
                "email": "verification.email@example.com",
                "verificationCode": "000000"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/verify/email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized());

        User reloaded = userRepository
                .findByEmail("verification.email@example.com")
                .orElseThrow();

        assertThat(reloaded.getEmailVerifiedAt())
                .isNull();
    }

    @Test
    void phoneVerificationIsRateLimited() throws Exception {

        User user = new User(
                "+919876543226",
                "verification.rate@example.com"
        );

        userRepository.save(user);

        verificationTokenService.createToken(
                user,
                VerificationTokenType.PHONE_VERIFICATION
        );

        String request = """
            {
                "phoneNumber": "+919876543226",
                "verificationCode": "000000"
            }
            """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/api/v1/auth/verify/phone")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(request)
                    )
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void registerRejectsInvalidPhoneNumber() throws Exception {

        String request = """
            {
                "phoneNumber": "919876543227",
                "email": "validation.phone@example.com",
                "pin": "TestPin123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {

        String request = """
            {
                "phoneNumber": "+919876543227",
                "email": "not-an-email",
                "pin": "TestPin123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsPinShorterThanSixCharacters() throws Exception {

        String request = """
            {
                "phoneNumber": "+919876543227",
                "email": "validation.pin.short@example.com",
                "pin": "12345"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsPinContainingNonAlphanumericCharacters() throws Exception {

        String request = """
            {
                "phoneNumber": "+919876543228",
                "email": "validation.pin.characters@example.com",
                "pin": "Test@123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsInvalidPhoneFormat() throws Exception {

        String request = """
            {
                "phoneNumber": "invalid-phone",
                "pin": "TestPin123"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificationRejectsInvalidCodeFormat() throws Exception {

        String request = """
            {
                "phoneNumber": "+919876543229",
                "verificationCode": "12345"
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/verify/phone")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshRejectsBlankRefreshToken() throws Exception {

        String request = """
            {
                "refreshToken": ""
            }
            """;

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCredentialsReturnGenericAuthenticationError() throws Exception {

        String request = """
            {
                "phoneNumber": "+919876543230",
                "pin": "WrongPin123"
            }
            """;

        String response = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response)
                .contains("\"code\":\"AUTHENTICATION_FAILED\"")
                .contains("\"message\":\"Authentication failed\"")
                .doesNotContain("Invalid credentials");
    }
}
