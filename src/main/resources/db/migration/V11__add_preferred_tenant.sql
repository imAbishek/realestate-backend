-- Preferred tenant for RENT / PG listings.
-- Nullable: existing listings (and SALE listings) keep NULL.
ALTER TABLE properties ADD COLUMN preferred_tenant VARCHAR(20);
