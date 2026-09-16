package com.sitms.settings;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link AppSetting} (SITMS-058). */
@Service
public class AppSettingService {

  public static final String SETTING_NOT_FOUND = "SETTING_NOT_FOUND";
  public static final String INVALID_SETTING_VALUE = "INVALID_SETTING_VALUE";

  private final AppSettingRepository appSettingRepository;

  public AppSettingService(AppSettingRepository appSettingRepository) {
    this.appSettingRepository = appSettingRepository;
  }

  @Transactional(readOnly = true)
  public Page<AppSettingResponse> list(Pageable pageable) {
    return appSettingRepository.findAllByOrderByDisplayNameAsc(pageable).map(AppSettingResponse::from);
  }

  /** Updates a setting's value, validating it against the setting's declared type. */
  @Transactional
  public AppSettingResponse update(String key, AppSettingRequest request) {
    AppSetting setting = find(key);
    validate(request.value(), setting.getType());
    setting.updateValue(request.value());
    return AppSettingResponse.from(appSettingRepository.save(setting));
  }

  /** Reads a single setting by key for the public-facing read endpoint. */
  @Transactional(readOnly = true)
  public AppSettingResponse findByKey(String key) {
    return appSettingRepository
        .findByKey(key)
        .map(AppSettingResponse::from)
        .orElseThrow(
            () -> new ResourceNotFoundException(SETTING_NOT_FOUND, "Setting not found: " + key));
  }

  /** Reads a boolean setting; unknown or unparsable values fall back to {@code defaultValue}. */
  @Transactional(readOnly = true)
  public boolean isEnabled(String key, boolean defaultValue) {
    return appSettingRepository
        .findByKey(key)
        .map(s -> Boolean.parseBoolean(s.getValue()))
        .orElse(defaultValue);
  }

  private void validate(String value, AppSettingType type) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(INVALID_SETTING_VALUE, "A setting value is required.");
    }
    boolean valid =
        switch (type) {
          case BOOLEAN -> value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
          case NUMBER -> {
            try {
              Long.parseLong(value.trim());
              yield true;
            } catch (NumberFormatException e) {
              yield false;
            }
          }
          case STRING -> true;
        };
    if (!valid) {
      throw new BusinessException(
          INVALID_SETTING_VALUE, "Value is not valid for a " + type + " setting.");
    }
  }

  private AppSetting find(String key) {
    return appSettingRepository
        .findByKey(key)
        .orElseThrow(
            () -> new ResourceNotFoundException(SETTING_NOT_FOUND, "Setting not found: " + key));
  }
}