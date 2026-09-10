CREATE TABLE expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        description TEXT NOT NULL,
                        category_id INTEGER NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        scope TEXT NOT NULL,
                        occurred_at TEXT NOT NULL,
                        credit_card_invoice_id INTEGER,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_expenses_category
                            FOREIGN KEY (category_id) REFERENCES categories(id),
                        CONSTRAINT fk_expenses_credit_card_invoice
                            FOREIGN KEY (credit_card_invoice_id) REFERENCES credit_card_invoices(id),
                        CONSTRAINT chk_expenses_amount
                            CHECK (amount_cents > 0),
                        CONSTRAINT chk_expenses_scope
                            CHECK (scope IN ('PERSONAL', 'HOUSEHOLD'))
);

CREATE INDEX idx_expenses_category_id
    ON expenses(category_id);

CREATE INDEX idx_expenses_credit_card_invoice_id
    ON expenses(credit_card_invoice_id);

CREATE INDEX idx_expenses_scope
    ON expenses(scope);

CREATE INDEX idx_expenses_occurred_at
    ON expenses(occurred_at);

CREATE TABLE expense_allocations (
                                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                                  expense_id INTEGER NOT NULL,
                                  person_id INTEGER NOT NULL,
                                  amount_cents INTEGER NOT NULL,
                                  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT fk_expense_allocations_expense
                                      FOREIGN KEY (expense_id) REFERENCES expenses(id),
                                  CONSTRAINT fk_expense_allocations_person
                                      FOREIGN KEY (person_id) REFERENCES people(id),
                                  CONSTRAINT uq_expense_allocations_expense_person
                                      UNIQUE (expense_id, person_id),
                                  CONSTRAINT chk_expense_allocations_amount
                                      CHECK (amount_cents > 0)
);

CREATE INDEX idx_expense_allocations_expense_id
    ON expense_allocations(expense_id);

CREATE INDEX idx_expense_allocations_person_id
    ON expense_allocations(person_id);
