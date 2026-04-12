-- Hibernate 6 maps Java enums to VARCHAR by default.
-- PostgreSQL native ENUM types cause type mismatch errors on insert.
-- Convert ALL enum-typed columns to VARCHAR so Hibernate can write them directly.
-- PostgreSQL enums cast implicitly to text, so no USING clause needed.

-- users
ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);

-- properties (5 enum columns)
ALTER TABLE properties ALTER COLUMN listing_type   TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN property_type  TYPE VARCHAR(30);
ALTER TABLE properties ALTER COLUMN price_unit     TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN furnishing     TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN status         TYPE VARCHAR(30);

-- inquiries
ALTER TABLE inquiries ALTER COLUMN status TYPE VARCHAR(20);

-- subscriptions
ALTER TABLE subscriptions ALTER COLUMN plan_type TYPE VARCHAR(20);

-- search_alerts (2 enum columns)
ALTER TABLE search_alerts ALTER COLUMN listing_type  TYPE VARCHAR(20);
ALTER TABLE search_alerts ALTER COLUMN property_type TYPE VARCHAR(30);
