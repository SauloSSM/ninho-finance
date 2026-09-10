CREATE TABLE bank_accounts (
                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                               name TEXT NOT NULL,
                               institution TEXT NOT NULL,
                               owner_id INTEGER NOT NULL,
                               initial_balance_cents INTEGER NOT NULL DEFAULT 0,
                               active INTEGER NOT NULL DEFAULT 1,
                               created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_bank_accounts_owner
                                   FOREIGN KEY (owner_id) REFERENCES people(id),

                               CONSTRAINT chk_bank_accounts_active
                                   CHECK (active IN (0, 1))
);

CREATE INDEX idx_bank_accounts_owner_id
    ON bank_accounts(owner_id);

CREATE INDEX idx_bank_accounts_active
    ON bank_accounts(active);
