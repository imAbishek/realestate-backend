-- ── Seed: localities for remaining cities ───────────────────────────────────
-- V5__seed_remaining_localities.sql
-- Adds localities for Chennai, Madurai, Bangalore, Hyderabad, Mumbai, Delhi, Pune.
-- Coimbatore localities were seeded in V2; this file covers all others.

-- Chennai
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Anna Nagar',       'anna-nagar',       13.0850, 80.2101),
    ('T. Nagar',         't-nagar',          13.0418, 80.2341),
    ('Adyar',            'adyar',            13.0012, 80.2565),
    ('Velachery',        'velachery',        12.9815, 80.2209),
    ('Porur',            'porur',            13.0359, 80.1569),
    ('Perambur',         'perambur',         13.1143, 80.2329),
    ('Chromepet',        'chromepet',        12.9516, 80.1462),
    ('Sholinganallur',   'sholinganallur',   12.9010, 80.2279),
    ('Ambattur',         'ambattur',         13.0982, 80.1647),
    ('Mylapore',         'mylapore',         13.0335, 80.2686)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'chennai';

-- Madurai
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Anna Nagar',      'anna-nagar',      9.9252,  78.1198),
    ('KK Nagar',        'kk-nagar',        9.9285,  78.1094),
    ('Iyer Bungalow',   'iyer-bungalow',   9.9461,  78.1262),
    ('Simmakkal',       'simmakkal',       9.9195,  78.1235),
    ('Thirumangalam',   'thirumangalam',   9.9795,  78.0802),
    ('Avaniyapuram',    'avaniyapuram',    9.8870,  78.1183),
    ('Tirupparankundram','tirupparankundram',9.9019, 78.0786),
    ('Palanganatham',   'palanganatham',   9.8914,  78.1465)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'madurai';

-- Bangalore
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Koramangala',     'koramangala',     12.9352, 77.6245),
    ('Indiranagar',     'indiranagar',     12.9784, 77.6408),
    ('Whitefield',      'whitefield',      12.9698, 77.7499),
    ('Electronic City', 'electronic-city', 12.8399, 77.6770),
    ('HSR Layout',      'hsr-layout',      12.9116, 77.6474),
    ('Bannerghatta Road','bannerghatta-road',12.8784,77.5965),
    ('Jayanagar',       'jayanagar',       12.9253, 77.5932),
    ('Marathahalli',    'marathahalli',    12.9591, 77.6974),
    ('BTM Layout',      'btm-layout',      12.9165, 77.6101),
    ('JP Nagar',        'jp-nagar',        12.9077, 77.5857)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'bangalore';

-- Hyderabad
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Hitech City',     'hitech-city',     17.4435, 78.3772),
    ('Gachibowli',      'gachibowli',      17.4401, 78.3489),
    ('Banjara Hills',   'banjara-hills',   17.4138, 78.4284),
    ('Jubilee Hills',   'jubilee-hills',   17.4318, 78.4072),
    ('Kondapur',        'kondapur',        17.4609, 78.3544),
    ('Madhapur',        'madhapur',        17.4477, 78.3924),
    ('Kukatpally',      'kukatpally',      17.4948, 78.3996),
    ('Miyapur',         'miyapur',         17.4985, 78.3611),
    ('LB Nagar',        'lb-nagar',        17.3490, 78.5527),
    ('Secunderabad',    'secunderabad',    17.4400, 78.4982)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'hyderabad';

-- Mumbai
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Andheri West',    'andheri-west',    19.1367, 72.8296),
    ('Andheri East',    'andheri-east',    19.1136, 72.8697),
    ('Bandra West',     'bandra-west',     19.0596, 72.8295),
    ('Powai',           'powai',           19.1176, 72.9060),
    ('Thane',           'thane',           19.2183, 72.9781),
    ('Navi Mumbai',     'navi-mumbai',     19.0330, 73.0297),
    ('Borivali',        'borivali',        19.2312, 72.8566),
    ('Malad',           'malad',           19.1872, 72.8484),
    ('Goregaon',        'goregaon',        19.1663, 72.8526),
    ('Dadar',           'dadar',           19.0178, 72.8478)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'mumbai';

-- Delhi
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Dwarka',          'dwarka',          28.5921, 77.0460),
    ('Rohini',          'rohini',          28.7347, 77.1149),
    ('Saket',           'saket',           28.5214, 77.2090),
    ('Vasant Kunj',     'vasant-kunj',     28.5255, 77.1580),
    ('Janakpuri',       'janakpuri',       28.6258, 77.0841),
    ('Lajpat Nagar',    'lajpat-nagar',    28.5677, 77.2433),
    ('Mayur Vihar',     'mayur-vihar',     28.6085, 77.2950),
    ('Karol Bagh',      'karol-bagh',      28.6514, 77.1908),
    ('Pitampura',       'pitampura',       28.7058, 77.1311),
    ('Shahdara',        'shahdara',        28.6729, 77.2880)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'delhi';

-- Pune
INSERT INTO localities (city_id, name, slug, latitude, longitude)
SELECT c.id, l.name, l.slug, l.lat, l.lng
FROM (VALUES
    ('Kothrud',         'kothrud',         18.5074, 73.8077),
    ('Hinjewadi',       'hinjewadi',       18.5912, 73.7389),
    ('Viman Nagar',     'viman-nagar',     18.5679, 73.9143),
    ('Baner',           'baner',           18.5590, 73.7868),
    ('Aundh',           'aundh',           18.5587, 73.8089),
    ('Wakad',           'wakad',           18.5977, 73.7616),
    ('Hadapsar',        'hadapsar',        18.5018, 73.9260),
    ('Kalyani Nagar',   'kalyani-nagar',   18.5451, 73.9023),
    ('Pimple Saudagar', 'pimple-saudagar', 18.6072, 73.8014),
    ('Shivajinagar',    'shivajinagar',    18.5308, 73.8475)
) AS l(name, slug, lat, lng)
CROSS JOIN cities c WHERE c.slug = 'pune';
