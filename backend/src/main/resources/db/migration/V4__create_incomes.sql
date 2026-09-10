CREATE TABLE incomes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        person_id INTEGER NOT NULL,
                        bank_account_id INTEGER,
                        category_id INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        expected_date TEXT NOT NULL,
                        received_at TEXT,
                        status TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_incomes_person
                            FOREIGN KEY (person_id) REFERENCES people(id),
                        CONSTRAINT fk_incomes_bank_account
                            FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id),
                        CONSTRAINT fk_incomes_category
                            FOREIGN KEY (category_id) REFERENCES categories(id),
                        CONSTRAINT chk_incomes_amount
                            CHECK (amount_cents > 0),
                        CONSTRAINT chk_incomes_status
                            CHECK (status IN ('EXPECTED', 'RECEIVED', 'CANCELLED')),
                        CONSTRAINT chk_incomes_received_at
                            CHECK (status != 'RECEIVED' OR received_at IS NOT NULL)
);

CREATE INDEX idx_incomes_person_id
    ON incomes(person_id);

CREATE INDEX idx_incomes_bank_account_id
    ON incomes(bank_account_id);

CREATE INDEX idx_incomes_category_id
    ON incomes(category_id);

CREATE INDEX idx_incomes_status
    ON incomes(status);

CREATE INDEX idx_incomes_expected_date
    ON incomes(expected_date);
