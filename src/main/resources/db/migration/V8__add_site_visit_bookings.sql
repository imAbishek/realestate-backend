-- ============================================================
-- V8__add_site_visit_bookings.sql
-- Phase D — site-visit booking entity.
-- Buyers (logged-in OR guest) request a visit slot from the
-- property detail page; owners confirm/cancel via inquiries flow
-- (admin moderation comes later).
--
-- status flow:  REQUESTED → CONFIRMED → COMPLETED
--                    └──────→ CANCELLED (by either party)
-- ============================================================

CREATE TABLE site_visit_bookings (
    id                UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id       UUID         NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    user_id           UUID                  REFERENCES users(id)      ON DELETE SET NULL,

    -- Snapshot of contact details at request time (guests have no user_id,
    -- and even logged-in users sometimes want to be reached differently).
    contact_name      VARCHAR(150) NOT NULL,
    contact_phone     VARCHAR(15),
    contact_email     VARCHAR(255),

    -- Free-text scheduling — full date/time picker is post-launch polish.
    preferred_date    VARCHAR(40),
    preferred_window  VARCHAR(60),
    notes             TEXT,

    -- REQUESTED | CONFIRMED | COMPLETED | CANCELLED
    status            VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    cancel_reason     TEXT,
    cancelled_by      VARCHAR(20),  -- BUYER | OWNER | ADMIN

    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_site_visits_property   ON site_visit_bookings(property_id);
CREATE INDEX idx_site_visits_user       ON site_visit_bookings(user_id);
CREATE INDEX idx_site_visits_status     ON site_visit_bookings(status);
CREATE INDEX idx_site_visits_created_at ON site_visit_bookings(created_at);

CREATE TRIGGER trg_site_visits_updated_at
    BEFORE UPDATE ON site_visit_bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
