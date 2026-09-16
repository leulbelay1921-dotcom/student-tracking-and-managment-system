-- =====================================================================
-- SITMS V19: Appearance app setting
--
-- Adds a global 'appearance' app setting that drives the portal look:
-- 'solid' (default), 'frosted', or 'glass'. It is readable by any
-- authenticated user so the applied look can be rendered app-wide while
-- only ADMINs can change it from the Settings page.
-- =====================================================================

INSERT INTO app_setting (setting_key, display_name, setting_type, setting_value, description)
VALUES ('appearance', 'Appearance', 'STRING', 'solid',
        'Portal look: solid, frosted, or glass.');