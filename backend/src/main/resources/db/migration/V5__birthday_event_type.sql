-- US-07 lists birthdays among the event types an organizer can pick, but the catalog
-- only had barbecue, baby shower, wedding and "other". Adds it with 10 suggestions.
-- Same convention as V2: English code, Portuguese names, example prices in cents.

INSERT INTO event_type (code, name, default_reservation_ttl_minutes) VALUES
    ('BIRTHDAY', 'Aniversário', 30);

INSERT INTO item (name, unit, unit_price_cents, archived, is_default, event_type_code) VALUES
    ('Bolo',                 'unidade', 15000, FALSE, TRUE, 'BIRTHDAY'),
    ('Salgadinhos',          'cento',    8000, FALSE, TRUE, 'BIRTHDAY'),
    ('Docinhos',             'cento',    9000, FALSE, TRUE, 'BIRTHDAY'),
    ('Refrigerante',         'fardo',    4500, FALSE, TRUE, 'BIRTHDAY'),
    ('Suco',                 'caixa',    3000, FALSE, TRUE, 'BIRTHDAY'),
    ('Cerveja',              'caixa',    9500, FALSE, TRUE, 'BIRTHDAY'),
    ('Decoração',            'kit',     12000, FALSE, TRUE, 'BIRTHDAY'),
    ('Balões',               'pacote',   2500, FALSE, TRUE, 'BIRTHDAY'),
    ('Velas e topo de bolo', 'kit',      3500, FALSE, TRUE, 'BIRTHDAY'),
    ('Lembrancinhas',        'kit',      6000, FALSE, TRUE, 'BIRTHDAY');
