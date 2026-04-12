-- Fix: sort_order in property_images was created as SMALLINT but JPA entity maps it to INTEGER
ALTER TABLE property_images ALTER COLUMN sort_order TYPE INTEGER;
