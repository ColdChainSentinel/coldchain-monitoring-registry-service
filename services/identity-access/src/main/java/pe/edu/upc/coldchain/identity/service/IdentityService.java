package pe.edu.upc.coldchain.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.coldchain.identity.config.IdentityProperties;
import pe.edu.upc.coldchain.identity.dto.CreateUserRequest;
import pe.edu.upc.coldchain.identity.dto.IntrospectResponse;
import pe.edu.upc.coldchain.identity.dto.LoginRequest;
import pe.edu.upc.coldchain.identity.dto.LoginResponse;
import pe.edu.upc.coldchain.identity.dto.UpdatePermissionsRequest;
import pe.edu.upc.coldchain.identity.dto.UserResponse;
import pe.edu.upc.coldchain.identity.exception.DuplicateUsernameException;
import pe.edu.upc.coldchain.identity.exception.ForbiddenAccessException;
import pe.edu.upc.coldchain.identity.exception.InvalidCredentialsException;
import pe.edu.upc.coldchain.identity.exception.UnauthorizedSessionException;
import pe.edu.upc.coldchain.identity.exception.UserNotFoundException;
import pe.edu.upc.coldchain.identity.model.Role;
import pe.edu.upc.coldchain.identity.model.UserAccount;
import pe.edu.upc.coldchain.identity.model.UserSession;
import pe.edu.upc.coldchain.identity.repository.UserAccountRepository;
import pe.edu.upc.coldchain.identity.repository.UserSessionRepository;

@Service
public class IdentityService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserAccountRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final IdentityProperties properties;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IdentityService(UserAccountRepository userRepository, UserSessionRepository sessionRepository,
            IdentityProperties properties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        UserAccount user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        String rawToken = generateToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.sessionTtl());
        UserSession session = new UserSession(UUID.randomUUID(), sha256(rawToken), user, now, expiresAt);
        sessionRepository.save(session);
        return new LoginResponse(
                rawToken,
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                Set.copyOf(user.getAllowedPointIds()),
                expiresAt,
                "Inicio de sesión exitoso");
    }

    @Transactional
    public void logout(String bearerHeader) {
        String token = extractBearerToken(bearerHeader);
        UserSession session = findValidSession(token);
        session.revoke();
        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public IntrospectResponse introspect(String bearerHeader, UUID monitoringPointId) {
        String token = extractBearerToken(bearerHeader);
        UserSession session = findValidSession(token);
        UserAccount user = session.getUser();
        if (monitoringPointId != null
                && user.getRole() != Role.ADMIN
                && !user.getAllowedPointIds().contains(monitoringPointId)) {
            throw new ForbiddenAccessException();
        }
        return new IntrospectResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                Set.copyOf(user.getAllowedPointIds()));
    }

    @Transactional(readOnly = true)
    public UserAccount requireAdmin(String bearerHeader) {
        String token = extractBearerToken(bearerHeader);
        UserSession session = findValidSession(token);
        UserAccount user = session.getUser();
        if (user.getRole() != Role.ADMIN) {
            throw new ForbiddenAccessException();
        }
        return user;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUsernameException();
        }
        UserAccount user = new UserAccount(
                UUID.randomUUID(),
                username,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.role(),
                true,
                Instant.now(),
                request.allowedPointIds());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll(Sort.by("username")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findUser(UUID id) {
        return UserResponse.from(userRepository.findById(id).orElseThrow(UserNotFoundException::new));
    }

    @Transactional
    public UserResponse updateStatus(UUID id, boolean active) {
        UserAccount user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        user.setActive(active);
        UserAccount saved = userRepository.save(user);
        if (!active) {
            List<UserSession> activeSessions = sessionRepository.findByUserIdAndRevokedFalse(id);
            for (UserSession session : activeSessions) {
                session.revoke();
            }
            sessionRepository.saveAll(activeSessions);
        }
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse updatePermissions(UUID id, UpdatePermissionsRequest request) {
        UserAccount user = userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        if (request.role() != null) {
            user.setRole(request.role());
        }
        user.replaceAllowedPointIds(request.allowedPointIds());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void ensureBootstrapUser(String username, String rawPassword, String fullName) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return;
        }
        if (!userRepository.existsByUsernameIgnoreCase(username.trim())) {
            UserAccount admin = new UserAccount(
                    UUID.randomUUID(),
                    username.trim(),
                    passwordEncoder.encode(rawPassword),
                    fullName != null && !fullName.isBlank() ? fullName.trim() : "Administrador ColdChain",
                    Role.ADMIN,
                    true,
                    Instant.now(),
                    Set.of());
            userRepository.save(admin);
        }
    }

    private UserSession findValidSession(String rawToken) {
        UserSession session = sessionRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(UnauthorizedSessionException::new);
        if (session.isRevoked() || !session.getExpiresAt().isAfter(Instant.now()) || !session.getUser().isActive()) {
            throw new UnauthorizedSessionException();
        }
        return session;
    }

    private static String extractBearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedSessionException();
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedSessionException();
        }
        return token;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
