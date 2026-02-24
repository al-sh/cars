CREATE TABLE cars (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand           VARCHAR(100)   NOT NULL,
    model           VARCHAR(100)   NOT NULL,
    year            INTEGER        NOT NULL CHECK (year BETWEEN 1990 AND 2030),
    price           INTEGER        NOT NULL CHECK (price > 0),
    body_type       VARCHAR(20)    NOT NULL CHECK (body_type IN ('SEDAN', 'SUV', 'HATCHBACK', 'WAGON', 'MINIVAN', 'COUPE', 'PICKUP')),
    engine_type     VARCHAR(20)    NOT NULL CHECK (engine_type IN ('PETROL', 'DIESEL', 'HYBRID', 'ELECTRIC')),
    engine_volume   NUMERIC(3, 1),
    power_hp        INTEGER        NOT NULL CHECK (power_hp > 0),
    transmission    VARCHAR(20)    NOT NULL CHECK (transmission IN ('MANUAL', 'AUTOMATIC', 'ROBOT', 'CVT')),
    drive           VARCHAR(10)    NOT NULL CHECK (drive IN ('FWD', 'RWD', 'AWD')),
    seats           INTEGER        NOT NULL CHECK (seats BETWEEN 2 AND 9),
    fuel_consumption NUMERIC(4, 1),
    description     TEXT,
    image_url       VARCHAR(500),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_cars_price      ON cars (price);
CREATE INDEX idx_cars_body_type  ON cars (body_type);
CREATE INDEX idx_cars_engine_type ON cars (engine_type);
CREATE INDEX idx_cars_brand      ON cars (brand);
CREATE INDEX idx_cars_year       ON cars (year);
CREATE INDEX idx_cars_seats      ON cars (seats);
CREATE INDEX idx_cars_search     ON cars (body_type, engine_type, price);
