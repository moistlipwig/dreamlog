CREATE TABLE dream_analysis (
    id UUID PRIMARY KEY,
    dream_id UUID NOT NULL REFERENCES dream_entry(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    summary VARCHAR(255),
    emotions JSONB,
    interpretation TEXT,
    risk_score DOUBLE PRECISION,
    recurring BOOLEAN,
    language VARCHAR(50),
    style VARCHAR(100),
    model_version VARCHAR(100)
);

CREATE TABLE dream_analysis_tags (
    dream_analysis_id UUID NOT NULL REFERENCES dream_analysis(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);

CREATE TABLE dream_analysis_entities (
    dream_analysis_id UUID NOT NULL REFERENCES dream_analysis(id) ON DELETE CASCADE,
    entity VARCHAR(255) NOT NULL
);
