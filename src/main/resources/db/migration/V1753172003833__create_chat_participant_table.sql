CREATE TABLE IF NOT EXISTS chat_participants (
    chat_id BIGINT NOT NULL
        CONSTRAINT chat_participants_chat_id_fk_key
        REFERENCES chats(id),
    phone_number VARCHAR(15) NOT NULL
        CONSTRAINT chat_participants_phone_number_fk_key
        REFERENCES users(phone_number),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (chat_id, phone_number)
);

CREATE INDEX IF NOT EXISTS idx_chat_participants_chat_id ON chat_participants(chat_id);
CREATE INDEX IF NOT EXISTS idx_chat_participants_phone_number ON chat_participants(phone_number);