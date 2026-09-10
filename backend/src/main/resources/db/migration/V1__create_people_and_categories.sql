CREATE TABLE people (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        active INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT chk_people_type
                            CHECK (type IN ('USER', 'EXTERNAL')),

                        CONSTRAINT chk_people_active
                            CHECK (active IN (0, 1))
);

CREATE TABLE categories (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            type TEXT NOT NULL,
                            active INTEGER NOT NULL DEFAULT 1,
                            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT uq_categories_name_type
                                UNIQUE (name, type),

                            CONSTRAINT chk_categories_type
                                CHECK (type IN ('EXPENSE', 'INCOME')),

                            CONSTRAINT chk_categories_active
                                CHECK (active IN (0, 1))
);

CREATE INDEX idx_people_type
    ON people(type);

CREATE INDEX idx_categories_type
    ON categories(type);