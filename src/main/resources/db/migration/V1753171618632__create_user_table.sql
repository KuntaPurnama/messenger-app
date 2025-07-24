CREATE TABLE IF NOT EXISTS users (
    phone_number VARCHAR(15) PRIMARY KEY,
    username VARCHAR(20),
    last_seen_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_users_phone_number ON users(phone_number);