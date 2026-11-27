package pe.edu.upc.coldchain.identity.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserAccount {
    @Id
    private UUID id;

    @Column(nullable = false, length = 60, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_point_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "monitoring_point_id", nullable = false)
    private Set<UUID> allowedPointIds = new HashSet<>();

    protected UserAccount() {
    }

    public UserAccount(UUID id, String username, String passwordHash, String fullName, Role role,
            boolean active, Instant createdAt, Set<UUID> allowedPointIds) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
        if (allowedPointIds != null) {
            this.allowedPointIds = new HashSet<>(allowedPointIds);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<UUID> getAllowedPointIds() {
        return allowedPointIds;
    }

    public void replaceAllowedPointIds(Set<UUID> pointIds) {
        this.allowedPointIds.clear();
        if (pointIds != null) {
            this.allowedPointIds.addAll(pointIds);
        }
    }
}
