-- Event types and 10 suggested items each (editable by the organizer before creating a list).
-- Codes are English; item names are Portuguese because they are user-facing data. Prices are examples.

INSERT INTO event_type (code, name, default_reservation_ttl_minutes) VALUES
    ('BARBECUE',    'Churrasco',        30),
    ('BABY_SHOWER', 'Chá de bebê',      30),
    ('WEDDING',     'Casamento',        30),
    ('OTHER',       'Outro',            30);

INSERT INTO template_item (event_type_code, position, name, unit, unit_price_cents, suggested_quotas) VALUES
    ('BARBECUE',  1, 'Picanha',              'kg',          9000, 4),
    ('BARBECUE',  2, 'Linguiça toscana',     'kg',          3500, 4),
    ('BARBECUE',  3, 'Pão de alho',          'pacote',      1800, 4),
    ('BARBECUE',  4, 'Carvão',               'saco de 5 kg', 2500, 4),
    ('BARBECUE',  5, 'Cerveja',              'caixa',       9500, 6),
    ('BARBECUE',  6, 'Refrigerante',         'fardo',       4500, 4),
    ('BARBECUE',  7, 'Gelo',                 'saco de 5 kg', 1200, 4),
    ('BARBECUE',  8, 'Farofa pronta',        'pacote',       900, 4),
    ('BARBECUE',  9, 'Vinagrete',            'porção',      1500, 3),
    ('BARBECUE', 10, 'Sobremesa',            'bandeja',     4000, 2),

    ('BABY_SHOWER',  1, 'Pacote de fraldas RN',  'pacote',   4500, 6),
    ('BABY_SHOWER',  2, 'Pacote de fraldas P',   'pacote',   5500, 6),
    ('BABY_SHOWER',  3, 'Pacote de fraldas M',   'pacote',   6500, 6),
    ('BABY_SHOWER',  4, 'Lenços umedecidos',     'caixa',    1800, 8),
    ('BABY_SHOWER',  5, 'Kit de banho',          'kit',      9000, 3),
    ('BABY_SHOWER',  6, 'Body e macacão',        'kit',      8000, 4),
    ('BABY_SHOWER',  7, 'Mamadeira',             'unidade',  4500, 3),
    ('BABY_SHOWER',  8, 'Cobertor',              'unidade',  7000, 2),
    ('BABY_SHOWER',  9, 'Móbile',                'unidade',  9000, 2),
    ('BABY_SHOWER', 10, 'Carrinho de bebê',      'unidade', 60000, 10),

    ('WEDDING',  1, 'Jogo de panelas',        'jogo',     60000, 6),
    ('WEDDING',  2, 'Jogo de cama',           'jogo',     35000, 4),
    ('WEDDING',  3, 'Aparelho de jantar',     'jogo',     45000, 5),
    ('WEDDING',  4, 'Liquidificador',         'unidade',  25000, 3),
    ('WEDDING',  5, 'Air fryer',              'unidade',  60000, 6),
    ('WEDDING',  6, 'Jogo de toalhas',        'jogo',     20000, 4),
    ('WEDDING',  7, 'Cafeteira',              'unidade',  30000, 3),
    ('WEDDING',  8, 'Taças de cristal',       'jogo',     28000, 4),
    ('WEDDING',  9, 'Aspirador robô',         'unidade', 120000, 10),
    ('WEDDING', 10, 'Cota lua de mel',        'cota',     10000, 20);
