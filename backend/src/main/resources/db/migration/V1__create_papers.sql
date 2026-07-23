CREATE TABLE papers (
    id UUID PRIMARY KEY,
    arxiv_id VARCHAR(64) NOT NULL UNIQUE,
    source_url VARCHAR(512) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    file_status VARCHAR(32) NOT NULL,
    processing_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
