CREATE TABLE media_files (
    id VARCHAR(255) PRIMARY KEY,
    owner_key VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
