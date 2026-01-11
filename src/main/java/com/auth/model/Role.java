package com.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Example: ROLE_ADMIN, ROLE_USER
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
