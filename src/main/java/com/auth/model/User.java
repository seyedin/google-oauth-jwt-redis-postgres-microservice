package com.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is the user in database.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /**
     * This is the id of user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * This is the username for login.
     * It must be unique and not blank.
     */
    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    /**
     * This is the password of user.
     * It is stored as hash.
     * It must not be blank.
     */
    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
    @Column(nullable = false)
    private String password;

    /**
     * This is the email of user.
     * It is unique if it is set.
     * For Google user, this is Google email.
     */
    @Email(message = "Email must be valid")
    @Size(max = 150, message = "Email must be at most 150 characters")
    @Column(unique = true, length = 150)
    private String email;

    /**
     * This is provider name.
     * Example: LOCAL or GOOGLE.
     */
    @NotBlank(message = "Provider must not be blank")
    @Pattern(regexp = "LOCAL|GOOGLE", message = "Provider must be LOCAL or GOOGLE")
    @Size(max = 20, message = "Provider must be at most 20 characters")
    @Column(name = "provider", length = 20)
    private String provider; // e.g. "LOCAL", "GOOGLE"

    /**
     * This is provider id from external provider.
     * Example: Google sub.
     */
    @Size(max = 100, message = "Provider id must be at most 100 characters")
    @Column(name = "provider_id", length = 100)
    private String providerId; // sub from Google

    /**
     * This is the role of user.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * This is the time when user is created.
     */
    private LocalDateTime createdAt;

    /**
     * This is the time when user is updated.
     */
    private LocalDateTime updatedAt;

    /**
     * This method sets times at insert.
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (provider == null || provider.isBlank()) {
            provider = "LOCAL";
        }
    }

    /**
     * This method sets update time.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // اگر فیلد enabled داری می‌توانی همان را برگردانی
    }
}
