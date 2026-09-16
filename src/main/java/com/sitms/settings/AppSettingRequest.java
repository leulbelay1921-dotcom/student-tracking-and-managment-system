package com.sitms.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for updating an {@link AppSetting}. */
public record AppSettingRequest(@NotBlank @Size(max = 2000) String value) {}