CREATE TABLE members (
    user_key VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    bio VARCHAR(255),
    status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE', 'WITHDRAWN')),
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_member_email UNIQUE (email)
);
