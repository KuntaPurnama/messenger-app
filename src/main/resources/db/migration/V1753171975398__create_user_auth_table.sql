CREATE TABLE IF NOT EXISTS user_auths (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(15) NOT NULL
        CONSTRAINT user_auths_phone_number_fk_key
        REFERENCES users(phone_number),
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_user_auths_phone_number ON user_auths(phone_number);