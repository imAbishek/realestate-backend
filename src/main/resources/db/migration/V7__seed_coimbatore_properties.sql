-- ============================================================
-- V7__seed_coimbatore_properties.sql
-- Seed a batch of realistic Coimbatore listings so the mobile
-- app (locked to Coimbatore for Phase B) has something to show
-- out of the box.
--
-- Creates one demo seller (demo.seller@propfind.in / Demo@1234)
-- and ~10 ACTIVE listings across SALE / RENT / PG / PLOT /
-- AGRI / COMMERCIAL — exercising the Phase B columns where
-- relevant. Three are marked featured.
--
-- Idempotent: every INSERT uses ON CONFLICT DO NOTHING or a
-- NOT EXISTS guard, so re-running this on a partially-seeded DB
-- (Neon already had some data) won't trip uniqueness.
-- ============================================================

-- ── 1. Demo seller user ─────────────────────────────────────
-- Password is Demo@1234 (bcrypt cost 10).
INSERT INTO users (name, email, phone, password_hash, role, is_verified, is_active)
VALUES (
    'PropFind Demo Seller',
    'demo.seller@propfind.in',
    '9876543210',
    '$2b$10$EtKOh6Y/sn13E52cySENIugwc1P0Utt7Mamr0Z/yIsauzH/ZmfP2q',
    'SELLER',
    TRUE,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

-- ── 2. Properties ───────────────────────────────────────────
-- Each row joins on the demo seller + the city/locality slugs.
-- We guard with NOT EXISTS (title, owner) so re-running can't
-- create duplicates.

WITH seller AS (
    SELECT id FROM users WHERE email = 'demo.seller@propfind.in'
),
loc AS (
    SELECT l.id, l.slug
    FROM localities l JOIN cities c ON c.id = l.city_id
    WHERE c.slug = 'coimbatore'
)
INSERT INTO properties (
    owner_id, locality_id, listing_type, property_type, title, description,
    price, price_unit, price_negotiable, security_deposit,
    bedrooms, bathrooms, balconies, total_floors, floor_number,
    area_sqft, carpet_area_sqft, furnishing, facing, age_of_property,
    parking_available, address_line, latitude, longitude,
    status, is_featured, is_verified, expires_at,
    listed_by, ownership_type,
    plot_length_ft, plot_breadth_ft, plot_area_cents, road_width_ft,
    boundary_wall, corner_plot, approval_authority,
    soil_type, water_source, has_well, electric_service, crop_currently_grown, fenced,
    promoter_project_name, promoter_years_experience, promoter_total_projects,
    promoter_cities_active, promoter_rera_id
)
SELECT
    s.id, l.id, p.listing_type, p.property_type,
    p.title, p.description,
    p.price, p.price_unit, p.price_negotiable, p.security_deposit,
    p.bedrooms, p.bathrooms, p.balconies, p.total_floors, p.floor_number,
    p.area_sqft, p.carpet_area_sqft, p.furnishing, p.facing, p.age_of_property,
    p.parking_available, p.address_line, p.latitude, p.longitude,
    'ACTIVE', p.is_featured, p.is_verified, NOW() + INTERVAL '90 days',
    p.listed_by, p.ownership_type,
    p.plot_length_ft, p.plot_breadth_ft, p.plot_area_cents, p.road_width_ft,
    p.boundary_wall, p.corner_plot, p.approval_authority,
    p.soil_type, p.water_source, p.has_well, p.electric_service, p.crop_currently_grown, p.fenced,
    p.promoter_project_name,
    p.promoter_years_experience::smallint,
    p.promoter_total_projects::smallint,
    p.promoter_cities_active, p.promoter_rera_id
FROM seller s
CROSS JOIN (VALUES
    -- ── Residential SALE ──────────────────────────────────
    ('rs-puram', 'SALE', 'APARTMENT',
        '3BHK in RS Puram with park view',
        'Spacious 3BHK in the heart of RS Puram. Walking distance to Cross Cut Road, schools and the Race Course. East-facing with cross ventilation and covered car parking.',
        9500000::numeric, 'TOTAL', TRUE, NULL,
        3::smallint, 3::smallint, 2::smallint, 5::smallint, 3::smallint,
        1450::numeric, 1320::numeric, 'SEMI_FURNISHED', 'East', 4::smallint,
        TRUE, '12, North Huzur Road, RS Puram', 11.0094::numeric, 76.9628::numeric,
        TRUE, TRUE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    ('saibaba-colony', 'SALE', 'APARTMENT',
        '2BHK ready-to-move in Saibaba Colony',
        '2BHK on the 2nd floor of a 4-storey gated community. Power backup, lift and 24x7 security.',
        6200000::numeric, 'TOTAL', TRUE, NULL,
        2::smallint, 2::smallint, 1::smallint, 4::smallint, 2::smallint,
        1050::numeric, 950::numeric, 'UNFURNISHED', 'North', 2::smallint,
        TRUE, 'NSR Road, Saibaba Colony', 11.0169::numeric, 76.9558::numeric,
        FALSE, TRUE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    ('peelamedu', 'SALE', 'VILLA',
        '4BHK independent villa near PSG Tech',
        'Brand-new 4BHK duplex villa on a 2400 sqft plot. Two-car parking, private garden, modular kitchen. Close to PSG Tech and Hindustan college.',
        18500000::numeric, 'TOTAL', FALSE, NULL,
        4::smallint, 4::smallint, 2::smallint, 2::smallint, 0::smallint,
        2400::numeric, 2200::numeric, 'SEMI_FURNISHED', 'East', 0::smallint,
        TRUE, 'Avinashi Road extn, Peelamedu', 11.0267::numeric, 77.0270::numeric,
        TRUE, TRUE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    ('hopes-college', 'SALE', 'INDEPENDENT_HOUSE',
        'Independent 3BHK house near Hopes College',
        '15-year old well-maintained independent house on a 1800 sqft plot. Bore well + corporation water, two-wheeler + car parking.',
        7800000::numeric, 'TOTAL', TRUE, NULL,
        3::smallint, 2::smallint, 1::smallint, 1::smallint, 0::smallint,
        1800::numeric, 1620::numeric, 'UNFURNISHED', 'West', 15::smallint,
        TRUE, 'Hope College, Avinashi Road', 11.0271::numeric, 76.9601::numeric,
        FALSE, FALSE, 'OWNER', 'INHERITED',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    -- ── Residential RENT ──────────────────────────────────
    ('gandhipuram', 'RENT', 'APARTMENT',
        '2BHK furnished flat near Gandhipuram bus stand',
        'Fully furnished 2BHK on the 1st floor, walking distance to Gandhipuram bus stand and central market.',
        22000::numeric, 'PER_MONTH', FALSE, 60000::numeric,
        2::smallint, 2::smallint, 1::smallint, 3::smallint, 1::smallint,
        1100::numeric, 990::numeric, 'FULLY_FURNISHED', 'South', 6::smallint,
        TRUE, '100 Feet Road, Gandhipuram', 11.0168::numeric, 76.9558::numeric,
        FALSE, TRUE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    ('tidel-park', 'RENT', 'APARTMENT',
        '3BHK semi-furnished apartment near Tidel Park',
        'Premium 3BHK gated community apartment near Tidel Park — ideal for IT professionals. Clubhouse, gym, and swimming pool.',
        35000::numeric, 'PER_MONTH', TRUE, 100000::numeric,
        3::smallint, 3::smallint, 2::smallint, 12::smallint, 7::smallint,
        1650::numeric, 1500::numeric, 'SEMI_FURNISHED', 'East', 3::smallint,
        TRUE, 'Vilankurichi Road, near Tidel Park', 11.0255::numeric, 77.0210::numeric,
        TRUE, TRUE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    -- ── PG ────────────────────────────────────────────────
    ('peelamedu', 'PG', 'PG_HOSTEL',
        'Ladies PG near PSG / Hindustan College',
        'Twin-sharing AC rooms with attached bath. Includes 3 meals, wifi, daily housekeeping. Walking distance to PSG and Hindustan.',
        8500::numeric, 'PER_MONTH', FALSE, 17000::numeric,
        NULL, NULL, NULL, NULL, NULL,
        180::numeric, NULL, 'FULLY_FURNISHED', NULL, NULL,
        FALSE, 'Trichy Road, Peelamedu', 11.0267::numeric, 77.0270::numeric,
        FALSE, FALSE, 'OWNER', 'SINGLE',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    -- ── Commercial ────────────────────────────────────────
    ('tidel-park', 'RENT', 'COMMERCIAL_OFFICE',
        '1200 sqft office space near Tidel Park',
        'Ready-to-move 1200 sqft office on the 4th floor with cubicles, cabins and pantry. Power backup + lift.',
        65000::numeric, 'PER_MONTH', TRUE, 200000::numeric,
        NULL, 2::smallint, NULL, 8::smallint, 4::smallint,
        1200::numeric, 1100::numeric, 'SEMI_FURNISHED', 'East', 5::smallint,
        TRUE, 'Vilankurichi Road, near Tidel Park', 11.0255::numeric, 77.0210::numeric,
        FALSE, TRUE, 'OWNER', 'COMPANY',
        NULL, NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    -- ── Plot / Land ───────────────────────────────────────
    ('ramanathapuram', 'SALE', 'PLOT',
        'DTCP approved 6-cent plot in Ramanathapuram',
        'Corner plot, DTCP approved, in a residential layout with compound wall and tarred road. Clear title, single owner.',
        4500000::numeric, 'TOTAL', TRUE, NULL,
        NULL, NULL, NULL, NULL, NULL,
        2613::numeric, NULL, 'UNFURNISHED', 'East', NULL,
        FALSE, 'Sundaram Nagar, Ramanathapuram', 11.0400::numeric, 76.9869::numeric,
        FALSE, TRUE, 'OWNER', 'SINGLE',
        60::numeric, 43.5::numeric, 6.0::numeric, 30::numeric,
        TRUE, TRUE, 'DTCP',
        NULL, NULL, NULL, NULL, NULL, NULL,
        NULL, NULL, NULL, NULL, NULL),

    -- ── Agricultural land ─────────────────────────────────
    ('singanallur', 'SALE', 'AGRICULTURAL_LAND',
        '2-acre coconut farm on Singanallur outskirts',
        'Productive 2-acre coconut farm with 120 yielding trees, borewell with 3-phase EB connection, and a small farmhouse. Patta available.',
        12000000::numeric, 'TOTAL', TRUE, NULL,
        NULL, NULL, NULL, NULL, NULL,
        87120::numeric, NULL, 'UNFURNISHED', NULL, NULL,
        FALSE, 'Off Trichy Road, Singanallur', 10.9975::numeric, 77.0276::numeric,
        FALSE, FALSE, 'OWNER', 'INHERITED',
        NULL, NULL, 200.0::numeric, 18::numeric,
        FALSE, FALSE, 'NONE',
        'RED', 'BOREWELL', TRUE, 'AVAILABLE_3PHASE', 'Coconut, banana', TRUE,
        NULL, NULL, NULL, NULL, NULL)
) AS p(
    locality_slug, listing_type, property_type, title, description,
    price, price_unit, price_negotiable, security_deposit,
    bedrooms, bathrooms, balconies, total_floors, floor_number,
    area_sqft, carpet_area_sqft, furnishing, facing, age_of_property,
    parking_available, address_line, latitude, longitude,
    is_featured, is_verified, listed_by, ownership_type,
    plot_length_ft, plot_breadth_ft, plot_area_cents, road_width_ft,
    boundary_wall, corner_plot, approval_authority,
    soil_type, water_source, has_well, electric_service, crop_currently_grown, fenced,
    promoter_project_name, promoter_years_experience, promoter_total_projects,
    promoter_cities_active, promoter_rera_id
)
JOIN loc l ON l.slug = p.locality_slug
WHERE NOT EXISTS (
    SELECT 1 FROM properties pr
    WHERE pr.owner_id = s.id AND pr.title = p.title
);

-- ── 3. Attach a couple of amenities to each seeded property ─────
-- Only for residential/PG/commercial — plots/agri stay without amenities.
INSERT INTO property_amenities (property_id, amenity_id)
SELECT pr.id, a.id
FROM properties pr
JOIN users u ON u.id = pr.owner_id AND u.email = 'demo.seller@propfind.in'
JOIN amenities a ON a.name IN (
    'Car Parking', 'Power Backup', 'CCTV Surveillance', 'Lift / Elevator', 'Security Guard'
)
WHERE pr.property_type NOT IN ('PLOT', 'AGRICULTURAL_LAND')
ON CONFLICT (property_id, amenity_id) DO NOTHING;
