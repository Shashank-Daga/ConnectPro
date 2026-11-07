-- ============================================================
-- ConnectPro - Standardize Indian phone numbers
-- Migration: V13
--
-- Convert legacy +91XXXXXXXXXX numbers to XXXXXXXXXX.
-- Apply to users, emergency contacts and notification history.
-- ============================================================

-- Validate existing data before making any changes.
-- If unexpected data is found, fail safely instead of
-- silently modifying an unknown phone-number format.

DO $$
BEGIN

    IF EXISTS (
        SELECT 1
        FROM users
        WHERE phone_number !~ '^\+91[0-9]{10}$'
    ) THEN
        RAISE EXCEPTION
            'V13 aborted: unexpected phone number format in users';
END IF;

    IF EXISTS (
        SELECT 1
        FROM emergency_contacts
        WHERE phone_number !~ '^\+91[0-9]{10}$'
    ) THEN
        RAISE EXCEPTION
            'V13 aborted: unexpected phone number format in emergency_contacts';
END IF;

    IF EXISTS (
        SELECT 1
        FROM trusted_contact_notifications
        WHERE recipient_phone_number !~ '^\+91[0-9]{10}$'
    ) THEN
        RAISE EXCEPTION
            'V13 aborted: unexpected phone number format in trusted_contact_notifications';
END IF;

    IF EXISTS (
        SELECT substring(phone_number FROM 4)
        FROM users
        GROUP BY substring(phone_number FROM 4)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'V13 aborted: duplicate phone numbers after normalization';
END IF;

END $$;


-- Convert account phone numbers.
UPDATE users
SET phone_number = substring(phone_number FROM 4);


-- Convert trusted-contact phone numbers.
UPDATE emergency_contacts
SET phone_number = substring(phone_number FROM 4);


-- Convert historical notification recipient numbers.
UPDATE trusted_contact_notifications
SET recipient_phone_number = substring(recipient_phone_number FROM 4);


-- Enforce exactly 10 digits at database level.
ALTER TABLE users
    ADD CONSTRAINT chk_users_phone_number_ten_digits
        CHECK (phone_number ~ '^[0-9]{10}$');


ALTER TABLE emergency_contacts
    ADD CONSTRAINT chk_emergency_contacts_phone_number_ten_digits
        CHECK (phone_number ~ '^[0-9]{10}$');


ALTER TABLE trusted_contact_notifications
    ADD CONSTRAINT chk_trusted_notifications_phone_number_ten_digits
        CHECK (recipient_phone_number ~ '^[0-9]{10}$');
