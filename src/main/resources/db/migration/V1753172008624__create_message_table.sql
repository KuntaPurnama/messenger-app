CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL
        CONSTRAINT messages_chat_id_fk_key
        REFERENCES chats(id),
    sender_phone_number VARCHAR(15) NOT NULL
        CONSTRAINT messages_sender_phone_number_fk_key
        REFERENCES users(phone_number),
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_id ON messages(chat_id);