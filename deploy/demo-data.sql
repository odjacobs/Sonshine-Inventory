-- Demo data for Sonshine Room Food Pantry
-- Run against sonshine_inventory database after Flyway migrations

-- Categories
INSERT INTO categories (id, name, active, display_order) VALUES
(1, 'Canned Goods',     1, 1),
(2, 'Dry Goods',        1, 2),
(3, 'Breakfast',        1, 3),
(4, 'Personal Care',    1, 4),
(5, 'Baby & Toddler',   1, 5);

-- Items (quantity = on hand, quota = monthly target)
INSERT INTO items (id, category_id, name, unit_label, quantity, quota, active) VALUES
-- Canned Goods
(1,  1, 'Canned Soup',           'cans',   14, 48, 1),
(2,  1, 'Canned Vegetables',     'cans',    8, 36, 1),
(3,  1, 'Canned Fruit',          'cans',   22, 36, 1),
(4,  1, 'Canned Beans',          'cans',    5, 48, 1),
(5,  1, 'Canned Tuna / Chicken', 'cans',    3, 24, 1),
-- Dry Goods
(6,  2, 'Pasta',                 'boxes',  11, 30, 1),
(7,  2, 'Rice',                  'lbs',     6, 40, 1),
(8,  2, 'Peanut Butter',         'jars',    4, 20, 1),
(9,  2, 'Jelly / Jam',           'jars',    9, 20, 1),
(10, 2, 'Pasta Sauce',           'jars',    2, 24, 1),
-- Breakfast
(11, 3, 'Cereal',                'boxes',  18, 30, 1),
(12, 3, 'Oatmeal',               'boxes',   7, 20, 1),
(13, 3, 'Pancake Mix',           'boxes',   1, 12, 1),
(14, 3, 'Syrup',                 'bottles', 0, 12, 1),
-- Personal Care
(15, 4, 'Shampoo',               'bottles', 6, 15, 1),
(16, 4, 'Toothpaste',            'tubes',   3, 20, 1),
(17, 4, 'Bar Soap',              'bars',   12, 30, 1),
(18, 4, 'Deodorant',             'sticks',  2, 15, 1),
-- Baby & Toddler
(19, 5, 'Diapers (Size 1-2)',    'packs',   0, 10, 1),
(20, 5, 'Diapers (Size 3-4)',    'packs',   1, 10, 1),
(21, 5, 'Baby Wipes',            'packs',   3, 12, 1),
(22, 5, 'Baby Formula',          'cans',    0,  8, 1);

-- Pledges (mix of OPEN, FULFILLED, EXPIRED)
INSERT INTO pledges (id, item_id, donor_name, donor_contact, quantity, status, created_at, expires_at, fulfilled_at, public_id) VALUES
-- OPEN pledges (active, covering some of the remaining need)
(1,  4,  'Margaret Sullivan',  'msullivan@email.com',  6, 'OPEN',      NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 6 DAY,  NULL, UUID()),
(2,  5,  'Robert Haines',      '555-0142',             4, 'OPEN',      NOW() - INTERVAL 2 DAY,  NOW() + INTERVAL 5 DAY,  NULL, UUID()),
(3,  7,  'Grace Community',    'office@gracechurch.org',10,'OPEN',     NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 6 DAY,  NULL, UUID()),
(4,  10, 'Tom & Lisa Park',    'tlpark@email.com',     6, 'OPEN',      NOW() - INTERVAL 3 DAY,  NOW() + INTERVAL 4 DAY,  NULL, UUID()),
(5,  13, 'Anita Flores',       'anita.flores@mail.com',4, 'OPEN',      NOW(),                   NOW() + INTERVAL 7 DAY,  NULL, UUID()),
(6,  14, 'First Baptist Youth','fbcyouth@fbc.org',     6, 'OPEN',      NOW(),                   NOW() + INTERVAL 7 DAY,  NULL, UUID()),
(7,  16, 'James Whitfield',    '555-0198',             8, 'OPEN',      NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 6 DAY,  NULL, UUID()),
(8,  18, 'Sandra Kim',         'sandrakim@email.com',  5, 'OPEN',      NOW() - INTERVAL 2 DAY,  NOW() + INTERVAL 5 DAY,  NULL, UUID()),
(9,  19, 'Calvary Women''s Group','calvary@cwg.org',   4, 'OPEN',      NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 6 DAY,  NULL, UUID()),
(10, 20, 'David Osei',         'dosei@email.com',      3, 'OPEN',      NOW(),                   NOW() + INTERVAL 7 DAY,  NULL, UUID()),
(11, 22, 'Hannah Reeves',      '555-0167',             3, 'OPEN',      NOW() - INTERVAL 1 DAY,  NOW() + INTERVAL 6 DAY,  NULL, UUID()),
-- FULFILLED pledges (historical)
(12, 1,  'Phil & Donna Carter', 'pcarter@email.com',  12, 'FULFILLED', NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 3 DAY,  NOW() - INTERVAL 4 DAY, UUID()),
(13, 2,  'Eastside Elementary', 'principal@eastside.edu',18,'FULFILLED',NOW() - INTERVAL 14 DAY,NOW() - INTERVAL 7 DAY,  NOW() - INTERVAL 8 DAY, UUID()),
(14, 6,  'Maria Gonzalez',      'mgonzalez@email.com', 8, 'FULFILLED', NOW() - INTERVAL 8 DAY,  NOW() - INTERVAL 1 DAY,  NOW() - INTERVAL 2 DAY, UUID()),
(15, 8,  'Boy Scout Troop 142', 'troop142@scouts.org',10, 'FULFILLED', NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 5 DAY,  NOW() - INTERVAL 6 DAY, UUID()),
-- EXPIRED pledges
(16, 3,  'Anonymous',           'anon@anon.com',        5, 'EXPIRED',  NOW() - INTERVAL 14 DAY, NOW() - INTERVAL 7 DAY,  NULL, UUID()),
(17, 11, 'Kevin Marsh',         '555-0133',             3, 'EXPIRED',  NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 3 DAY,  NULL, UUID());
