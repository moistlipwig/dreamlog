ALTER TABLE dream_entry
    ADD COLUMN search_vector tsvector;

CREATE INDEX idx_dream_entry_search_vector
    ON dream_entry
        USING GIN (search_vector);

-- Create GIN index for trigram fuzzy matching on title
-- Allows fuzzy matching for typos (e.g., "lucdi" → "lucid")
CREATE INDEX idx_dream_entry_title_trgm
    ON dream_entry
        USING GIN (title gin_trgm_ops);

CREATE INDEX idx_dream_entry_content_trgm
    ON dream_entry
        USING GIN (content gin_trgm_ops);

CREATE OR REPLACE FUNCTION dream_entry_search_vector_update() RETURNS trigger AS
$$
DECLARE
    tags_string TEXT;
BEGIN
    -- Fetch tags from dream_entry_tags table if this is an UPDATE
    -- (tags won't exist yet on INSERT, they're added after)
    IF TG_OP = 'UPDATE' THEN
        SELECT COALESCE(string_agg(tag, ' '), '')
        INTO tags_string
        FROM dream_entry_tags
        WHERE dream_entry_id = NEW.id;
    ELSE
        tags_string := '';
    END IF;

    NEW.search_vector :=
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.title, ''))), 'A') ||
        setweight(to_tsvector('simple', unaccent(COALESCE(NEW.content, ''))), 'B') ||
        setweight(to_tsvector('simple', unaccent(tags_string)), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger to automatically update search_vector on INSERT and UPDATE
-- Fires BEFORE INSERT/UPDATE so search_vector is populated before row is written
CREATE TRIGGER dream_entry_search_vector_trigger
    BEFORE INSERT OR UPDATE
    ON dream_entry
    FOR EACH ROW
EXECUTE FUNCTION dream_entry_search_vector_update();

-- Trigger function to update parent dream_entry when tags change
-- This ensures search_vector stays in sync with tags
CREATE OR REPLACE FUNCTION dream_entry_tags_update_search_vector() RETURNS trigger AS
$$
BEGIN
    -- Update the parent dream_entry to trigger search_vector refresh
    IF TG_OP = 'DELETE' THEN
        UPDATE dream_entry SET updated_at = NOW() WHERE id = OLD.dream_entry_id;
        RETURN OLD;
    ELSE
        UPDATE dream_entry SET updated_at = NOW() WHERE id = NEW.dream_entry_id;
        RETURN NEW;
    END IF;
END
$$ LANGUAGE plpgsql;

-- Trigger on dream_entry_tags to update parent search_vector
CREATE TRIGGER dream_entry_tags_search_vector_trigger
    AFTER INSERT OR UPDATE OR DELETE
    ON dream_entry_tags
    FOR EACH ROW
EXECUTE FUNCTION dream_entry_tags_update_search_vector();

-- Backfill existing data with search vectors
-- This updates all existing rows to populate search_vector
UPDATE dream_entry
SET updated_at = updated_at;

-- Add comment explaining the FTS strategy
COMMENT ON COLUMN dream_entry.search_vector IS
    'Automatically maintained tsvector for full-text search. Uses simple config + unaccent for Polish character support (ł→l, ą→a). Weights: A=title, B=content, C=tags. Updated by trigger on INSERT/UPDATE.';
