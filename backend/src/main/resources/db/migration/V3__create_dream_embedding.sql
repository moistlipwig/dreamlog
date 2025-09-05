CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE dream_embedding (
    dream_id UUID PRIMARY KEY REFERENCES dream_entry(id),
    vector VECTOR(1536) NOT NULL
);
