CREATE TABLE IF NOT EXISTS accounts
(
    account_id UUID PRIMARY KEY,
    amount     NUMERIC(15, 2) NOT NULL DEFAULT 0.00
);
