CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE dream_entry (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    mood_in_dream VARCHAR(32),
    mood_after_dream VARCHAR(32),
    vividness INT,
    lucid BOOLEAN
);

CREATE TABLE dream_entry_tags (
    dream_entry_id UUID NOT NULL REFERENCES dream_entry(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);
