package dev.folio.auth;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 254) public String email;
    @Column(nullable = false, length = 80) public String name;
    @Column(nullable = false) public String passwordHash;
    protected Account() {}
    public Account(String email, String name, String hash) { this.email = email; this.name = name; this.passwordHash = hash; }
}
