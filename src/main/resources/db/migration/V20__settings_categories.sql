-- =====================================================================
-- SITMS V20: Settings categories + extra seeded settings
--
-- Groups the Settings page into four functional sections: General,
-- Notifications, Security, and Appearance. Existing settings default to
-- GENERAL (appearance is re-tagged), and richer seeds are added so each
-- section has working controls on first load.
-- =====================================================================

ALTER TABLE app_setting
    ADD COLUMN setting_category VARCHAR(32) NOT NULL DEFAULT 'GENERAL';

UPDATE app_setting SET setting_category = 'APPEARANCE' WHERE setting_key = 'appearance';

INSERT INTO app_setting (setting_key, display_name, setting_type, setting_value, description, setting_category)
VALUES
    ('portal_name', 'Portal name', 'STRING', 'SITMS',
     'Name shown across the portal.', 'GENERAL'),
    ('email_notifications', 'Email notifications', 'BOOLEAN', 'true',
     'Send email notifications for portal activity.', 'NOTIFICATIONS'),
    ('application_alerts', 'Application alerts', 'BOOLEAN', 'true',
     'Notify when application statuses change.', 'NOTIFICATIONS'),
    ('pending_review_threshold_days', 'Review threshold (days)', 'NUMBER', '7',
     'Days before review reminders are sent.', 'NOTIFICATIONS'),
    ('session_timeout_minutes', 'Session timeout (minutes)', 'NUMBER', '60',
     'Idle minutes before a session expires.', 'SECURITY'),
    ('password_expiry_days', 'Password expiry (days)', 'NUMBER', '90',
     'Days before users must change their passwords.', 'SECURITY'),
    ('mfa_required', 'Require MFA', 'BOOLEAN', 'false',
     'Require multi-factor authentication for all users.', 'SECURITY');