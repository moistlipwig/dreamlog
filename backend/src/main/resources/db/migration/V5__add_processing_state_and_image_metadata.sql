-- Add processing state and image metadata columns to dream_entry
-- Processing state is TEXT - validation handled by Spring Boot
ALTER TABLE dream_entry
    ADD COLUMN processing_state   TEXT NOT NULL DEFAULT 'CREATED',
    ADD COLUMN image_uri          TEXT,
    ADD COLUMN image_storage_key  VARCHAR(255),
    ADD COLUMN image_generated_at TIMESTAMPTZ,
    ADD COLUMN failure_reason     TEXT,
    ADD COLUMN retry_count        INT  NOT NULL DEFAULT 0;
