-- One row per paper holding both agent outputs under a single status. Keeping them together means
-- claiming a paper for generation is one atomic UPDATE, so concurrent PDF accesses cannot start
-- duplicate generation runs.
CREATE TABLE paper_analyses (
    id UUID PRIMARY KEY,
    paper_id UUID NOT NULL UNIQUE REFERENCES papers(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,
    content_json JSONB,
    questionnaire_json JSONB,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
