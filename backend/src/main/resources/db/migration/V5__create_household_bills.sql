CREATE TABLE household_bills (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               name TEXT NOT NULL,
                               category_id INTEGER NOT NULL,
                               responsible_person_id INTEGER NOT NULL,
                               reference_year INTEGER NOT NULL,
                               reference_month INTEGER NOT NULL,
                               expected_amount_cents INTEGER NOT NULL,
                               actual_amount_cents INTEGER,
                               due_date TEXT NOT NULL,
                               status TEXT NOT NULL,
                               created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_household_bills_category
                                   FOREIGN KEY (category_id) REFERENCES categories(id),
                               CONSTRAINT fk_household_bills_responsible_person
                                   FOREIGN KEY (responsible_person_id) REFERENCES people(id),
                               CONSTRAINT chk_household_bills_reference_month
                                   CHECK (reference_month BETWEEN 1 AND 12),
                               CONSTRAINT chk_household_bills_expected_amount
                                   CHECK (expected_amount_cents >= 0),
                               CONSTRAINT chk_household_bills_actual_amount
                                   CHECK (actual_amount_cents IS NULL OR actual_amount_cents >= 0),
                               CONSTRAINT chk_household_bills_status
                                   CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'CANCELLED'))
);

CREATE INDEX idx_household_bills_category_id
    ON household_bills(category_id);

CREATE INDEX idx_household_bills_responsible_person_id
    ON household_bills(responsible_person_id);

CREATE INDEX idx_household_bills_reference
    ON household_bills(reference_year, reference_month);

CREATE INDEX idx_household_bills_status
    ON household_bills(status);

CREATE INDEX idx_household_bills_due_date
    ON household_bills(due_date);
