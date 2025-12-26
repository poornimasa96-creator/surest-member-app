-- Seed roles
INSERT INTO role (id, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ROLE_ADMIN'),
    ('22222222-2222-2222-2222-222222222222', 'ROLE_USER');

-- Seed users (password is 'password' hashed with BCrypt)
INSERT INTO "user" (id, username, password_hash, role_id) VALUES
    ('33333333-3333-3333-3333-333333333333', 'admin',
     '$2a$10$G3m/gUKKImoykV4DZjkTROTDE6WywpNHKmkI4eO0yGZ5Bb6PluNSW',
     '11111111-1111-1111-1111-111111111111'),
    ('44444444-4444-4444-4444-444444444444', 'user',
     '$2a$10$G3m/gUKKImoykV4DZjkTROTDE6WywpNHKmkI4eO0yGZ5Bb6PluNSW',
     '22222222-2222-2222-2222-222222222222');

-- Seed sample members
INSERT INTO member (first_name, last_name, date_of_birth, email) VALUES
    ('John', 'Doe', '1985-05-15', 'john.doe@example.com'),
    ('Jane', 'Smith', '1990-08-22', 'jane.smith@example.com'),
    ('Bob', 'Johnson', '1978-12-10', 'bob.johnson@example.com');
