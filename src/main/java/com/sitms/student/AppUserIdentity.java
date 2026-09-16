package com.sitms.student;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal access to the shared {@code app_user} identity table (SITMS-007).
 *
 * <p>A full {@code AppUser} JPA entity is out of scope for this ticket; this thin JdbcTemplate
 * helper creates the identity row that a {@link Student} profile links to. It is only invoked
 * inside a surrounding transaction.
 */
@Component
public class AppUserIdentity {

  private final JdbcTemplate jdbcTemplate;

  public AppUserIdentity(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Whether an {@code app_user} row already uses the given short id as its {@code username}. */
  public boolean existsByUsername(String username) {
    return findIdByUsername(username).isPresent();
  }

  /** The internal id of the {@code app_user} row using the given short id, if any. */
  public Optional<UUID> findIdByUsername(String username) {
    List<UUID> ids =
        jdbcTemplate.queryForList(
            "SELECT id FROM app_user WHERE username = ?", UUID.class, username);
    return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
  }

  /** Renames the {@code app_user} row's username (and derived email) for the given identity. */
  @Transactional
  public void rename(UUID appUserId, String newUsername) {
    jdbcTemplate.update(
        "UPDATE app_user SET username = ?, email = ? WHERE id = ?",
        newUsername,
        newUsername + "@sitms.local",
        appUserId);
  }

  /** Updates the {@code app_user} row's email so the identity stays in sync with the profile. */
  @Transactional
  public void updateEmail(UUID appUserId, String email) {
    jdbcTemplate.update("UPDATE app_user SET email = ? WHERE id = ?", email, appUserId);
  }

  /**
   * Creates a new {@code app_user} row with the given short id as its unique {@code username},
   * the given role and derived email, returning the generated internal UUID.
   */
  @Transactional
  public UUID ensureRole(String username, String fullName, String role) {
    return ensureRole(username, fullName, role, username + "@sitms.local");
  }

  @Transactional
  public UUID ensureRole(String username, String fullName, String role, String email) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  "INSERT INTO app_user (id, username, email, display_name, role, active, created_by)"
                      + " VALUES (?, ?, ?, ?, ?, TRUE, ?)");
          ps.setObject(1, id);
          ps.setString(2, username);
          ps.setString(3, email == null || email.isBlank() ? username + "@sitms.local" : email);
          ps.setString(4, fullName);
          ps.setString(5, role);
          ps.setObject(6, id);
          return ps;
        });
    return id;
  }

  /**
   * Creates a new {@code app_user} row with role {@code STUDENT}, keeping the caller of profile
   * types (e.g. {@link com.sitms.student.StudentService}) free of cross-package dependencies.
   */
  @Transactional
  public UUID ensure(String username, String fullName) {
    return ensureRole(username, fullName, "STUDENT");
  }
}