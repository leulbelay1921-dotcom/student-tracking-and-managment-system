package com.sitms.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A typed application setting exposed to admins on the Settings page (SITMS-058).
 *
 * <p>{@code key} is a stable machine-readable identifier, {@code displayName}/{@code description}
 * are the admin-facing labels, and {@code value} is stored as text and validated against its
 * {@link AppSettingType}. Audit columns and the optimistic-lock {@code version} follow the V1
 * convention. Rows are seeded by migration V14.
 */
@Entity
@Table(name = "app_setting")
@EntityListeners(AuditingEntityListener.class)
public class AppSetting {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "setting_key", nullable = false, unique = true, length = 128)
  private String key;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "setting_type", nullable = false, length = 32)
  private AppSettingType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "setting_category", nullable = false, length = 32)
  private AppSettingCategory category;

  @Column(name = "setting_value", length = 2000)
  private String value;

  @Column(name = "description", length = 255)
  private String description;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @CreatedBy
  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @LastModifiedBy
  @Column(name = "updated_by")
  private UUID updatedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected AppSetting() {}

  public UUID getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getDisplayName() {
    return displayName;
  }

  public AppSettingType getType() {
    return type;
  }

  public AppSettingCategory getCategory() {
    return category;
  }

  public String getValue() {
    return value;
  }

  public String getDescription() {
    return description;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  void updateValue(String value) {
    this.value = value;
  }
}