-- ============================================================
-- V6__add_plot_agri_promoter_columns.sql
-- Phase B (InvusProp wizard) — extend properties with nullable
-- columns for plot/land, agricultural land, and promoter listings.
-- API stays backward-compatible because every column is nullable.
-- Enum values stored as VARCHAR (V4 already converted enum cols).
-- ============================================================

-- ── Who is listing this property ────────────────────────────
-- OWNER | PROMOTER  (null = OWNER, for legacy rows)
ALTER TABLE properties ADD COLUMN listed_by VARCHAR(20);

-- ── Plot / Land fields ──────────────────────────────────────
ALTER TABLE properties ADD COLUMN plot_length_ft        DECIMAL(10,2);
ALTER TABLE properties ADD COLUMN plot_breadth_ft       DECIMAL(10,2);
ALTER TABLE properties ADD COLUMN plot_area_cents       DECIMAL(10,4);
ALTER TABLE properties ADD COLUMN road_width_ft         DECIMAL(6,2);
ALTER TABLE properties ADD COLUMN boundary_wall         BOOLEAN;
ALTER TABLE properties ADD COLUMN corner_plot           BOOLEAN;
-- DTCP | CMDA | TNHB | CMA | RERA | LOCAL | OTHER | NONE
ALTER TABLE properties ADD COLUMN approval_authority    VARCHAR(20);
-- SINGLE | JOINT | GIFT | INHERITED | COMPANY | TRUST
ALTER TABLE properties ADD COLUMN ownership_type        VARCHAR(20);

-- ── Agricultural land fields ────────────────────────────────
-- RED | BLACK | ALLUVIAL | LATERITE | SANDY | CLAY | LOAM | OTHER
ALTER TABLE properties ADD COLUMN soil_type             VARCHAR(20);
-- BOREWELL | OPEN_WELL | CANAL | RIVER | RAIN_FED | NONE
ALTER TABLE properties ADD COLUMN water_source          VARCHAR(20);
ALTER TABLE properties ADD COLUMN has_well              BOOLEAN;
-- AVAILABLE_3PHASE | AVAILABLE_1PHASE | AGRI_CONNECTION | NONE
ALTER TABLE properties ADD COLUMN electric_service      VARCHAR(30);
ALTER TABLE properties ADD COLUMN crop_currently_grown  VARCHAR(200);
ALTER TABLE properties ADD COLUMN fenced                BOOLEAN;

-- ── Promoter / Builder fields ───────────────────────────────
ALTER TABLE properties ADD COLUMN promoter_project_name     VARCHAR(200);
ALTER TABLE properties ADD COLUMN promoter_years_experience SMALLINT;
ALTER TABLE properties ADD COLUMN promoter_total_projects   SMALLINT;
ALTER TABLE properties ADD COLUMN promoter_cities_active    TEXT;
ALTER TABLE properties ADD COLUMN promoter_rera_id          VARCHAR(60);

-- ── Property documents (FMB sketch, Encumbrance Certificate, etc.) ──
CREATE TABLE property_documents (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    property_id  UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    -- FMB_SKETCH | EC | PATTA | APPROVAL_LETTER | OTHER
    doc_type     VARCHAR(30) NOT NULL,
    url          TEXT        NOT NULL,
    label        VARCHAR(120),
    uploaded_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_property_documents_property ON property_documents(property_id);
