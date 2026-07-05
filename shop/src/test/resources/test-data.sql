-- Test-only seed: the integration tests authenticate as userId=1 (see BaseIntegrationTest.auth),
-- and carts.user_id has a FK to users(id). Without this row, inserting a cart row fails with
-- a referential-integrity violation (HTTP 500). Items are seeded by the shared data.sql.
INSERT INTO users (id, login, password, authorities) VALUES (1, 'user', 'password', 'USER');
