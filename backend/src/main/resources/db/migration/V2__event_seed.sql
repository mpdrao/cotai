-- Events and 10 suggested items each (editable by the organizer before creating a list).
-- Suggested items are Item rows with is_default = TRUE linked to the Event through event_code.
-- Codes are English; item names are Portuguese because they are user-facing data. Prices are examples.

INSERT INTO event (code, name, default_reservation_ttl_minutes) VALUES
    ('BARBECUE',    'Churrasco',   30),
    ('BABY_SHOWER', 'Chá de bebê', 30),
    ('WEDDING',     'Casamento',   30),
    ('BIRTHDAY',    'Aniversário', 30),
    ('OTHER',       'Outro',       30);

INSERT INTO item (event_code, name, unit, unit_price_cents, archived, is_default) VALUES
    ('BARBECUE', 'Picanha',          'kg',           9000, FALSE, TRUE),
    ('BARBECUE', 'Linguiça toscana', 'kg',           3500, FALSE, TRUE),
    ('BARBECUE', 'Pão de alho',      'pacote',       1800, FALSE, TRUE),
    ('BARBECUE', 'Carvão',           'saco de 5 kg', 2500, FALSE, TRUE),
    ('BARBECUE', 'Cerveja',          'caixa',        9500, FALSE, TRUE),
    ('BARBECUE', 'Refrigerante',     'fardo',        4500, FALSE, TRUE),
    ('BARBECUE', 'Gelo',             'saco de 5 kg', 1200, FALSE, TRUE),
    ('BARBECUE', 'Farofa pronta',    'pacote',        900, FALSE, TRUE),
    ('BARBECUE', 'Vinagrete',        'porção',       1500, FALSE, TRUE),
    ('BARBECUE', 'Sobremesa',        'bandeja',      4000, FALSE, TRUE),

    ('BABY_SHOWER', 'Pacote de fraldas RN', 'pacote',   4500, FALSE, TRUE),
    ('BABY_SHOWER', 'Pacote de fraldas P',  'pacote',   5500, FALSE, TRUE),
    ('BABY_SHOWER', 'Pacote de fraldas M',  'pacote',   6500, FALSE, TRUE),
    ('BABY_SHOWER', 'Lenços umedecidos',    'caixa',    1800, FALSE, TRUE),
    ('BABY_SHOWER', 'Kit de banho',         'kit',      9000, FALSE, TRUE),
    ('BABY_SHOWER', 'Body e macacão',       'kit',      8000, FALSE, TRUE),
    ('BABY_SHOWER', 'Mamadeira',            'unidade',  4500, FALSE, TRUE),
    ('BABY_SHOWER', 'Cobertor',             'unidade',  7000, FALSE, TRUE),
    ('BABY_SHOWER', 'Móbile',               'unidade',  9000, FALSE, TRUE),
    ('BABY_SHOWER', 'Carrinho de bebê',     'unidade', 60000, FALSE, TRUE),

    ('WEDDING', 'Jogo de panelas',    'jogo',     60000, FALSE, TRUE),
    ('WEDDING', 'Jogo de cama',       'jogo',     35000, FALSE, TRUE),
    ('WEDDING', 'Aparelho de jantar', 'jogo',     45000, FALSE, TRUE),
    ('WEDDING', 'Liquidificador',     'unidade',  25000, FALSE, TRUE),
    ('WEDDING', 'Air fryer',          'unidade',  60000, FALSE, TRUE),
    ('WEDDING', 'Jogo de toalhas',    'jogo',     20000, FALSE, TRUE),
    ('WEDDING', 'Cafeteira',          'unidade',  30000, FALSE, TRUE),
    ('WEDDING', 'Taças de cristal',   'jogo',     28000, FALSE, TRUE),
    ('WEDDING', 'Aspirador robô',     'unidade', 120000, FALSE, TRUE),
    ('WEDDING', 'Cota lua de mel',    'cota',     10000, FALSE, TRUE),

    ('BIRTHDAY', 'Bolo',                 'unidade', 15000, FALSE, TRUE),
    ('BIRTHDAY', 'Salgadinhos',          'cento',    8000, FALSE, TRUE),
    ('BIRTHDAY', 'Docinhos',             'cento',    9000, FALSE, TRUE),
    ('BIRTHDAY', 'Refrigerante',         'fardo',    4500, FALSE, TRUE),
    ('BIRTHDAY', 'Suco',                 'caixa',    3000, FALSE, TRUE),
    ('BIRTHDAY', 'Cerveja',              'caixa',    9500, FALSE, TRUE),
    ('BIRTHDAY', 'Decoração',            'kit',     12000, FALSE, TRUE),
    ('BIRTHDAY', 'Balões',               'pacote',   2500, FALSE, TRUE),
    ('BIRTHDAY', 'Velas e topo de bolo', 'kit',      3500, FALSE, TRUE),
    ('BIRTHDAY', 'Lembrancinhas',        'kit',      6000, FALSE, TRUE);
