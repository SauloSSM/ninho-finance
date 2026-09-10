CREATE TABLE credit_cards (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              name TEXT NOT NULL,
                              institution TEXT NOT NULL,
                              holder_id INTEGER NOT NULL,
                              invoice_payer_id INTEGER NOT NULL,
                              default_responsible_person_id INTEGER NOT NULL,
                              last_four TEXT,
                              credit_limit_cents INTEGER NOT NULL,
                              closing_day INTEGER NOT NULL,
                              due_day INTEGER NOT NULL,
                              active INTEGER NOT NULL DEFAULT 1,
                              created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT fk_credit_cards_holder
                                  FOREIGN KEY (holder_id) REFERENCES people(id),
                              CONSTRAINT fk_credit_cards_invoice_payer
                                  FOREIGN KEY (invoice_payer_id) REFERENCES people(id),
                              CONSTRAINT fk_credit_cards_default_responsible_person
                                  FOREIGN KEY (default_responsible_person_id) REFERENCES people(id),
                              CONSTRAINT chk_credit_cards_credit_limit
                                  CHECK (credit_limit_cents >= 0),
                              CONSTRAINT chk_credit_cards_last_four
                                  CHECK (last_four IS NULL OR last_four GLOB '[0-9][0-9][0-9][0-9]'),
                              CONSTRAINT chk_credit_cards_closing_day
                                  CHECK (closing_day BETWEEN 1 AND 31),
                              CONSTRAINT chk_credit_cards_due_day
                                  CHECK (due_day BETWEEN 1 AND 31),
                              CONSTRAINT chk_credit_cards_active
                                  CHECK (active IN (0, 1))
);

CREATE INDEX idx_credit_cards_holder_id
    ON credit_cards(holder_id);

CREATE INDEX idx_credit_cards_invoice_payer_id
    ON credit_cards(invoice_payer_id);

CREATE INDEX idx_credit_cards_active
    ON credit_cards(active);

CREATE TABLE credit_card_invoices (
                                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                                      credit_card_id INTEGER NOT NULL,
                                      reference_year INTEGER NOT NULL,
                                      reference_month INTEGER NOT NULL,
                                      due_date TEXT NOT NULL,
                                      status TEXT NOT NULL,
                                      reported_total_cents INTEGER,
                                      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_credit_card_invoices_credit_card
                                          FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id),
                                      CONSTRAINT uq_credit_card_invoices_card_reference
                                          UNIQUE (credit_card_id, reference_year, reference_month),
                                      CONSTRAINT chk_credit_card_invoices_reference_month
                                          CHECK (reference_month BETWEEN 1 AND 12),
                                      CONSTRAINT chk_credit_card_invoices_status
                                          CHECK (status IN ('OPEN', 'CLOSED')),
                                      CONSTRAINT chk_credit_card_invoices_reported_total
                                          CHECK (reported_total_cents IS NULL OR reported_total_cents >= 0)
);

CREATE INDEX idx_credit_card_invoices_credit_card_id
    ON credit_card_invoices(credit_card_id);
