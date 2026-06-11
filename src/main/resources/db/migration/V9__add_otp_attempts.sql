-- Tracks failed OTP guesses per account so we can lock the OTP after a few
-- wrong tries (verify-email + password-reset brute-force defense).
-- Reset to 0 whenever a fresh OTP is issued or the OTP is cleared.
ALTER TABLE users
    ADD COLUMN otp_attempts INTEGER NOT NULL DEFAULT 0;
