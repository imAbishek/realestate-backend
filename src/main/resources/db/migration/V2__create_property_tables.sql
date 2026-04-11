-- ============================================================
-- V2__create_property_tables.sql
-- Flyway migration — runs after V1
-- Creates: cities, localities, amenities, properties,
--          property_images, property_amenities, inquiries,
--          saved_properties, reviews, subscriptions, search_alerts
-- ============================================================

-- ── Enums ────────────────────────────────────────────────────

CREATE TYPE listing_type AS ENUM ('SALE', 'RENT', 'PG');

CREATE TYPE property_type AS ENUM (
    'APARTMENT', 'INDEPENDENT_HOUSE', 'VILLA',
    'PLOT', 'COMMERCIAL_OFFICE', 'COMMERCIAL_SHOP',
    'BUILDER_FLOOR', 'PG_HOSTEL'
);

CREATE TYPE furnishing_status AS ENUM (
    'UNFURNISHED', 'SEMI_FURNISHED', 'FULLY_FURNISHED'
);

CREATE TYPE listing_status AS ENUM (
    'DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'EXPIRED', 'REJECTED', 'SOLD_RENTED'
);

CREATE TYPE price_unit AS ENUM ('TOTAL', 'PER_MONTH', 'PER_SQFT');

CREATE TYPE inquiry_status AS ENUM ('new', 'read', 'replied', 'closed');

CREATE TYPE plan_type AS ENUM ('free', 'basic', 'premium', 'agent_pro');

-- ── Cities ───────────────────────────────────────────────────

CREATE TABLE cities (
    id         UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100)  NOT NULL,
    state      VARCHAR(100)  NOT NULL,
    slug       VARCHAR(120)  NOT NULL UNIQUE,
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_cities_slug ON cities(slug);

-- ── Localities ───────────────────────────────────────────────

CREATE TABLE localities (
    id         UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    city_id    UUID    NOT NULL REFERENCES cities(id) ON DELETE CASCADE,
    name       VARCHAR(150)  NOT NULL,
    slug       VARCHAR(180)  NOT NULL,
    latitude   DECIMAL(9,6),
    longitude  DECIMAL(9,6),
    is_active  BOOLEAN       NOT NULL DEFAULT TRUE,
    UNIQUE(city_id, slug)
);

CREATE INDEX idx_localities_city ON localities(city_id);

-- ── Amenities ────────────────────────────────────────────────

CREATE TABLE amenities (
    id        UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    name      VARCHAR(100) NOT NULL,
    category  VARCHAR(60),
    icon_key  VARCHAR(60)
);

-- ── Properties ───────────────────────────────────────────────

CREATE TABLE properties (
    id                 UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id           UUID            NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    agent_id           UUID                     REFERENCES users(id)      ON DELETE SET NULL,
    locality_id        UUID            NOT NULL REFERENCES localities(id),
    listing_type       listing_type    NOT NULL,
    property_type      property_type   NOT NULL,
    title              VARCHAR(255)    NOT NULL,
    description        TEXT,
    price              DECIMAL(14,2)   NOT NULL,
    price_unit         price_unit               DEFAULT 'TOTAL',
    price_negotiable   BOOLEAN                  DEFAULT FALSE,
    security_deposit   DECIMAL(14,2),
    bedrooms           SMALLINT,
    bathrooms          SMALLINT,
    balconies          SMALLINT,
    total_floors       SMALLINT,
    floor_number       SMALLINT,
    area_sqft          DECIMAL(10,2)   NOT NULL,
    carpet_area_sqft   DECIMAL(10,2),
    furnishing         furnishing_status        DEFAULT 'UNFURNISHED',
    facing             VARCHAR(20),
    age_of_property    SMALLINT,
    available_from     DATE,
    parking_available  BOOLEAN                  DEFAULT FALSE,
    address_line       TEXT,
    latitude           DECIMAL(9,6),
    longitude          DECIMAL(9,6),
    status             listing_status  NOT NULL  DEFAULT 'PENDING_REVIEW',
    is_featured        BOOLEAN                   DEFAULT FALSE,
    is_verified        BOOLEAN                   DEFAULT FALSE,
    rejection_reason   TEXT,
    views_count        INT                       DEFAULT 0,
    inquiry_count      INT                       DEFAULT 0,
    expires_at         TIMESTAMP,
    created_at         TIMESTAMP       NOT NULL  DEFAULT NOW(),
    updated_at         TIMESTAMP       NOT NULL  DEFAULT NOW()
);

CREATE INDEX idx_properties_owner    ON properties(owner_id);
CREATE INDEX idx_properties_locality ON properties(locality_id);
CREATE INDEX idx_properties_status   ON properties(status);
CREATE INDEX idx_properties_type     ON properties(listing_type, property_type);
CREATE INDEX idx_properties_price    ON properties(price);
CREATE INDEX idx_properties_beds     ON properties(bedrooms);
CREATE INDEX idx_properties_featured ON properties(is_featured) WHERE is_featured = TRUE;

CREATE TRIGGER trg_properties_updated_at
    BEFORE UPDATE ON properties
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ── Property images ──────────────────────────────────────────

CREATE TABLE property_images (
    id           UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id  UUID    NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    url          TEXT    NOT NULL,
    is_primary   BOOLEAN             DEFAULT FALSE,
    sort_order   SMALLINT            DEFAULT 0,
    uploaded_at  TIMESTAMP           DEFAULT NOW()
);

CREATE INDEX idx_property_images ON property_images(property_id);

-- ── Property ↔ Amenities ─────────────────────────────────────

CREATE TABLE property_amenities (
    property_id  UUID NOT NULL REFERENCES properties(id)  ON DELETE CASCADE,
    amenity_id   UUID NOT NULL REFERENCES amenities(id)   ON DELETE CASCADE,
    PRIMARY KEY (property_id, amenity_id)
);

-- ── Inquiries ────────────────────────────────────────────────

CREATE TABLE inquiries (
    id           UUID             PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id  UUID             NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    sender_id    UUID                      REFERENCES users(id)      ON DELETE SET NULL,
    guest_name   VARCHAR(150),
    guest_email  VARCHAR(255),
    guest_phone  VARCHAR(15),
    message      TEXT             NOT NULL,
    status       inquiry_status             DEFAULT 'new',
    replied_at   TIMESTAMP,
    created_at   TIMESTAMP                  DEFAULT NOW()
);

CREATE INDEX idx_inquiries_property ON inquiries(property_id);
CREATE INDEX idx_inquiries_sender   ON inquiries(sender_id);

-- ── Saved properties ─────────────────────────────────────────

CREATE TABLE saved_properties (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    property_id  UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    saved_at     TIMESTAMP        DEFAULT NOW(),
    UNIQUE(user_id, property_id)
);

-- ── Subscriptions ────────────────────────────────────────────

CREATE TABLE subscriptions (
    id                   UUID       PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_type            plan_type  NOT NULL DEFAULT 'free',
    listing_limit        INT                 DEFAULT 1,
    featured_allowed     BOOLEAN             DEFAULT FALSE,
    razorpay_order_id    VARCHAR(100),
    razorpay_payment_id  VARCHAR(100),
    amount_paid          DECIMAL(10,2),
    starts_at            TIMESTAMP           DEFAULT NOW(),
    expires_at           TIMESTAMP,
    is_active            BOOLEAN             DEFAULT TRUE,
    created_at           TIMESTAMP           DEFAULT NOW()
);

-- ── Search alerts ────────────────────────────────────────────

CREATE TABLE search_alerts (
    id                UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id           UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    city_id           UUID                   REFERENCES cities(id),
    locality_id       UUID                   REFERENCES localities(id),
    listing_type      listing_type,
    property_type     property_type,
    min_price         DECIMAL(14,2),
    max_price         DECIMAL(14,2),
    min_bedrooms      SMALLINT,
    max_bedrooms      SMALLINT,
    is_active         BOOLEAN                DEFAULT TRUE,
    last_notified_at  TIMESTAMP,
    created_at        TIMESTAMP              DEFAULT NOW()
);

-- ── Seed: cities ─────────────────────────────────────────────

INSERT INTO cities (name, state, slug) VALUES
    ('Chennai',    'Tamil Nadu',  'chennai'),
    ('Coimbatore', 'Tamil Nadu',  'coimbatore'),
    ('Madurai',    'Tamil Nadu',  'madurai'),
    ('Bangalore',  'Karnataka',   'bangalore'),
    ('Hyderabad',  'Telangana',   'hyderabad'),
    ('Mumbai',     'Maharashtra', 'mumbai'),
    ('Delhi',      'Delhi',       'delhi'),
    ('Pune',       'Maharashtra', 'pune');

-- ── Seed: amenities ──────────────────────────────────────────

INSERT INTO amenities (name, category, icon_key) VALUES
    ('Swimming Pool',        'recreation', 'swimming-pool'),
    ('Gymnasium',            'recreation', 'gym'),
    ('Club House',           'recreation', 'club-house'),
    ('Children Play Area',   'recreation', 'play-area'),
    ('Jogging Track',        'recreation', 'jogging'),
    ('CCTV Surveillance',    'security',   'cctv'),
    ('Security Guard',       'security',   'guard'),
    ('Gated Community',      'security',   'gate'),
    ('Power Backup',         'utilities',  'power-backup'),
    ('Lift / Elevator',      'utilities',  'elevator'),
    ('Car Parking',          'utilities',  'parking'),
    ('Piped Gas',            'utilities',  'gas'),
    ('Rainwater Harvesting', 'utilities',  'rainwater'),
    ('Solar Panels',         'utilities',  'solar'),
    ('Wifi / Internet',      'utilities',  'wifi'),
    ('Fire Safety',          'safety',     'fire'),
    ('Intercom',             'safety',     'intercom'),
    ('Garden / Park',        'recreation', 'garden'),
    ('Tennis Court',         'recreation', 'tennis'),
    ('Basketball Court',     'recreation', 'basketball');

-- ── Seed: some Coimbatore localities ────────────────────────

INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('RS Puram',       'rs-puram',       11.0094, 76.9628),
    ('Saibaba Colony', 'saibaba-colony', 11.0169, 76.9558),
    ('Peelamedu',      'peelamedu',      11.0267, 77.0270),
    ('Gandhipuram',    'gandhipuram',    11.0168, 76.9558),
    ('Singanallur',    'singanallur',    10.9975, 77.0276),
    ('Hopes College',  'hopes-college',  11.0271, 76.9601),
    ('Tidel Park',     'tidel-park',     11.0255, 77.0210),
    ('Ramanathapuram', 'ramanathapuram', 11.0400, 76.9869)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'coimbatore';
