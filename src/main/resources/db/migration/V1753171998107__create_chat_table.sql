CREATE TABLE IF NOT EXISTS chats (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20),
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(15) NOT NULL
        CONSTRAINT chats_created_by_fk_key
        REFERENCES users(phone_number),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_chats_created_by ON chats(created_by);