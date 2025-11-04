-- Add processing state enum for tracking async AI pipeline progress
CREATE TYPE dream_processing_state AS ENUM (
    'CREATED',              -- Initial state after dream creation
    'ANALYZING_TEXT',       -- Text analysis task in progress
    'TEXT_ANALYZED',        -- Text analysis completed, before image generation
    'GENERATING_IMAGE',     -- Image generation task in progress
    'COMPLETED',            -- All processing done (analysis + image ready)
    'FAILED'                -- Unrecoverable failure after max retries
);

-- Add processing state and image metadata columns to dream_entry
ALTER TABLE dream_entry
    ADD COLUMN processing_state dream_processing_state NOT NULL DEFAULT 'CREATED',
    ADD COLUMN image_uri TEXT,                              -- Full URI to image (e.g., presigned URL or public endpoint)
    ADD COLUMN image_storage_key VARCHAR(255),              -- Object key in MinIO bucket (for internal operations)
    ADD COLUMN image_generated_at TIMESTAMPTZ,              -- Timestamp when image was generated
    ADD COLUMN failure_reason TEXT,                         -- Error message if state = FAILED
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0,          -- Number of retry attempts for failed tasks
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Index for querying by processing state (useful for monitoring and debugging)
CREATE INDEX idx_dream_entry_processing_state ON dream_entry(processing_state);

-- Index for querying failed dreams
CREATE INDEX idx_dream_entry_failed ON dream_entry(processing_state, updated_at) WHERE processing_state = 'FAILED';

-- Trigger to automatically update updated_at on row modification
CREATE OR REPLACE FUNCTION update_dream_entry_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_dream_entry_updated_at
    BEFORE UPDATE ON dream_entry
    FOR EACH ROW
    EXECUTE FUNCTION update_dream_entry_updated_at();
