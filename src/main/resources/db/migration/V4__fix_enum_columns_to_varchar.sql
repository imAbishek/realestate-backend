-- Hibernate 6 maps Java enums to VARCHAR by default.
-- PostgreSQL native ENUM types cause type mismatch errors on insert.
-- Convert all enum columns to VARCHAR so Hibernate can write them directly.

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(20);

ALTER TABLE properties ALTER COLUMN listing_type   TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN property_type  TYPE VARCHAR(30);
ALTER TABLE properties ALTER COLUMN price_unit     TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN furnishing     TYPE VARCHAR(20);
ALTER TABLE properties ALTER COLUMN status         TYPE VARCHAR(30);

ALTER TABLE inquiries ALTER COLUMN status TYPE VARCHAR(20);

ALTER TABLE seller_plans ALTER COLUMN plan_type TYPE VARCHAR(20);
