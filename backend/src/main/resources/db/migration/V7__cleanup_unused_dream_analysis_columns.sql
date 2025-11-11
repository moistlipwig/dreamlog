-- Remove unused columns from dream_analysis table
-- These columns were planned but not implemented in the current phase
-- Following YAGNI principle - can be re-added in future migrations if needed

ALTER TABLE dream_analysis
    DROP COLUMN IF EXISTS risk_score,           -- Mental health risk detection (not implemented)
    DROP COLUMN IF EXISTS recurring,            -- Recurring dream flag (not implemented)
    DROP COLUMN IF EXISTS language,             -- Detected language (not needed, using user's language)
    DROP COLUMN IF EXISTS style;                -- Dream narrative style (not in current scope)
