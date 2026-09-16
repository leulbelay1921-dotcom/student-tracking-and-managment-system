package com.sitms.settings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link AppSetting}. */
public interface AppSettingRepository extends JpaRepository<AppSetting, UUID> {

  Page<AppSetting> findAllByOrderByDisplayNameAsc(Pageable pageable);

  Optional<AppSetting> findByKey(String key);

  boolean existsByKey(String key);
}