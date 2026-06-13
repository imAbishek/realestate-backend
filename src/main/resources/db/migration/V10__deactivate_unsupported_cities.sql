-- PropFind launches city by city. Only Coimbatore is operational today, but V2
-- seeded eight cities all defaulting to is_active = TRUE, so the city picker
-- listed seven places we don't actually cover under "Available now".
-- Deactivate everything except Coimbatore; re-activate a city (its own migration)
-- when we genuinely launch there.

UPDATE cities SET is_active = FALSE WHERE slug <> 'coimbatore';
