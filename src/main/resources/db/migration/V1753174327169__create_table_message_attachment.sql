CREATE TABLE IF NOT EXISTS message_attachments (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL
        CONSTRAINT message_attachments_message_id_fk_key
        REFERENCES messages(id),
    file_url VARCHAR(255),
    file_type VARCHAR(20),
    file_size BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_message_attachments_message_id ON message_attachments(message_id);