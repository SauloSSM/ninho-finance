CREATE TABLE payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        payer_person_id INTEGER NOT NULL,
                        bank_account_id INTEGER,
                        method TEXT NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        paid_at TEXT NOT NULL,
                        expense_id INTEGER,
                        household_bill_id INTEGER,
                        credit_card_invoice_id INTEGER,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_payments_payer_person
                            FOREIGN KEY (payer_person_id) REFERENCES people(id),
                        CONSTRAINT fk_payments_bank_account
                            FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id),
                        CONSTRAINT fk_payments_expense
                            FOREIGN KEY (expense_id) REFERENCES expenses(id),
                        CONSTRAINT fk_payments_household_bill
                            FOREIGN KEY (household_bill_id) REFERENCES household_bills(id),
                        CONSTRAINT fk_payments_credit_card_invoice
                            FOREIGN KEY (credit_card_invoice_id) REFERENCES credit_card_invoices(id),
                        CONSTRAINT chk_payments_method
                            CHECK (method IN ('PIX', 'BOLETO', 'DEBIT', 'CASH', 'TRANSFER', 'OTHER')),
                        CONSTRAINT chk_payments_amount
                            CHECK (amount_cents > 0),
                        CONSTRAINT chk_payments_exactly_one_target
                            CHECK (
                                (expense_id IS NOT NULL) +
                                (household_bill_id IS NOT NULL) +
                                (credit_card_invoice_id IS NOT NULL) = 1
                            )
);

CREATE INDEX idx_payments_payer_person_id
    ON payments(payer_person_id);

CREATE INDEX idx_payments_bank_account_id
    ON payments(bank_account_id);

CREATE INDEX idx_payments_expense_id
    ON payments(expense_id);

CREATE INDEX idx_payments_household_bill_id
    ON payments(household_bill_id);

CREATE INDEX idx_payments_credit_card_invoice_id
    ON payments(credit_card_invoice_id);

CREATE TABLE settlement_payments (
                                   id INTEGER PRIMARY KEY AUTOINCREMENT,
                                   from_person_id INTEGER NOT NULL,
                                   to_person_id INTEGER NOT NULL,
                                   amount_cents INTEGER NOT NULL,
                                   paid_at TEXT NOT NULL,
                                   notes TEXT,
                                   created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_settlement_payments_from_person
                                       FOREIGN KEY (from_person_id) REFERENCES people(id),
                                   CONSTRAINT fk_settlement_payments_to_person
                                       FOREIGN KEY (to_person_id) REFERENCES people(id),
                                   CONSTRAINT chk_settlement_payments_amount
                                       CHECK (amount_cents > 0),
                                   CONSTRAINT chk_settlement_payments_distinct_people
                                       CHECK (from_person_id != to_person_id)
);

CREATE INDEX idx_settlement_payments_from_person_id
    ON settlement_payments(from_person_id);

CREATE INDEX idx_settlement_payments_to_person_id
    ON settlement_payments(to_person_id);

CREATE INDEX idx_settlement_payments_people
    ON settlement_payments(from_person_id, to_person_id);
