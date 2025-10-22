-- Extensions for UUID generation and full-text search
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- Required for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS unaccent;  -- Required for removing diacritics in search
CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- Required for trigram similarity search

-- Users table (core user entity)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Federated identities (OAuth providers: Google, Facebook, etc.)
CREATE TABLE federated_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL, -- 'google', 'facebook', etc.
    provider_user_id VARCHAR(255) NOT NULL, -- sub from OAuth provider
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_federated_user_id ON federated_identities(user_id);
CREATE UNIQUE INDEX idx_federated_provider_user ON federated_identities(provider, provider_user_id);

-- Local credentials (email/password authentication)
CREATE TABLE local_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_local_credentials_user ON local_credentials(user_id);

-- Dream entries
CREATE TABLE dream_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    mood_in_dream VARCHAR(32),
    mood_after_dream VARCHAR(32),
    vividness INT,
    lucid BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dream_entry_user_id ON dream_entry(user_id);
CREATE INDEX idx_dream_entry_date ON dream_entry(date DESC);

-- Dream entry tags
CREATE TABLE dream_entry_tags (
    dream_entry_id UUID NOT NULL REFERENCES dream_entry(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY (dream_entry_id, tag)
);

CREATE INDEX idx_dream_entry_tags_tag ON dream_entry_tags(tag);
