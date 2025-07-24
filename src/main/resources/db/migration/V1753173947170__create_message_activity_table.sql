CREATE TABLE IF NOT EXISTS message_activities (
    message_id BIGINT NOT NULL
        CONSTRAINT message_activities_message_id_fk_key
        REFERENCES messages(id),
    user_phone_number VARCHAR(15) NOT NULL
        CONSTRAINT message_activities_user_phone_number_fk_key
        REFERENCES users(phone_number),
    status VARCHAR(50),
    reaction VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (message_id, user_phone_number)
);

CREATE INDEX IF NOT EXISTS idx_message_activites_message_id ON message_activities(message_id);
CREATE INDEX IF NOT EXISTS idx_message_activites_message_id_status ON message_activities(message_id, status);