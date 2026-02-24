INSERT INTO cars (brand, model, year, price, body_type, engine_type, engine_volume, power_hp, transmission, drive, seats, fuel_consumption, description) VALUES

-- Кроссоверы
('Toyota',    'RAV4',       2023, 3500000, 'SUV',       'PETROL',   2.5, 199, 'AUTOMATIC', 'AWD', 5,  8.1, 'Популярный семейный кроссовер с надёжной репутацией'),
('Mazda',     'CX-5',       2023, 3200000, 'SUV',       'PETROL',   2.5, 194, 'AUTOMATIC', 'AWD', 5,  7.8, 'Стильный и управляемый кроссовер премиум-класса'),
('Kia',       'Sportage',   2023, 2900000, 'SUV',       'PETROL',   2.0, 150, 'AUTOMATIC', 'AWD', 5,  8.4, 'Современный кроссовер с богатой комплектацией'),
('Hyundai',   'Tucson',     2023, 2800000, 'SUV',       'PETROL',   2.0, 150, 'AUTOMATIC', 'AWD', 5,  8.2, 'Надёжный кроссовер корейского производства'),
('Toyota',    'Highlander', 2022, 5200000, 'SUV',       'HYBRID',   2.5, 243, 'AUTOMATIC', 'AWD', 7,  7.2, 'Трёхрядный гибридный внедорожник для большой семьи'),
('Haval',     'F7',         2023, 2100000, 'SUV',       'PETROL',   1.5, 150, 'ROBOT',     'AWD', 5,  8.0, 'Доступный кроссовер с полным приводом'),

-- Седаны
('Toyota',    'Camry',      2023, 3000000, 'SEDAN',     'PETROL',   2.5, 200, 'AUTOMATIC', 'FWD', 5,  8.5, 'Классический бизнес-седан с высоким уровнем комфорта'),
('Kia',       'K5',         2023, 2600000, 'SEDAN',     'PETROL',   2.0, 150, 'AUTOMATIC', 'FWD', 5,  7.9, 'Динамичный седан с современным дизайном'),
('Hyundai',   'Sonata',     2022, 2400000, 'SEDAN',     'PETROL',   2.0, 150, 'AUTOMATIC', 'FWD', 5,  8.0, 'Просторный семейный седан'),

-- Хэтчбеки
('Volkswagen','Golf',       2022, 2500000, 'HATCHBACK', 'PETROL',   1.4, 150, 'AUTOMATIC', 'FWD', 5,  6.5, 'Компактный городской автомобиль с отличной управляемостью'),
('Kia',       'Ceed',       2022, 2200000, 'HATCHBACK', 'PETROL',   1.6, 128, 'AUTOMATIC', 'FWD', 5,  6.9, 'Практичный хэтчбек для города'),

-- Минивэн
('Kia',       'Carnival',   2023, 4200000, 'MINIVAN',   'DIESEL',   2.2, 199, 'AUTOMATIC', 'FWD', 8,  7.5, 'Просторный минивэн для большой семьи'),

-- Электромобили
('Tesla',     'Model 3',    2023, 4500000, 'SEDAN',     'ELECTRIC', NULL, 283, 'AUTOMATIC', 'AWD', 5, NULL, 'Электрический седан с запасом хода 500 км'),
('Zeekr',     '001',        2023, 4800000, 'WAGON',     'ELECTRIC', NULL, 544, 'AUTOMATIC', 'AWD', 5, NULL, 'Мощный электрический универсал премиум-класса');
