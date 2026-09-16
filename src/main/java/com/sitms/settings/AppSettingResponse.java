package com.sitms.settings;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link AppSetting}. */
public record AppSettingResponse(
    UUID id,
    String key,
    String displayName,
    AppSettingType type,
    AppSettingCategory category,
    String value,
    String description,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AppSettingResponse from(AppSetting setting) {
    return new AppSettingResponse(
        setting.getId(),
        setting.getKey(),
        setting.getDisplayName(),
        setting.getType(),
        setting.getCategory(),
        setting.getValue(),
        setting.getDescription(),
        setting.getCreatedAt(),
        setting.getUpdatedAt());
  }
}