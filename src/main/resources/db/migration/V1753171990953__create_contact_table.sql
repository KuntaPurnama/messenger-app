CREATE TABLE IF NOT EXISTS contacts (
    user_phone_number VARCHAR(15) NOT NULL
        CONSTRAINT contacts_user_phone_number_fk_key
        REFERENCES users(phone_number),
    contact_phone_number VARCHAR(15) NOT NULL
        CONSTRAINT contacts_contact_phone_number_fk_key
        REFERENCES users(phone_number),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (user_phone_number, contact_phone_number)
);

CREATE INDEX IF NOT EXISTS idx_contacts_user_phone_number ON contacts(user_phone_number);
CREATE INDEX IF NOT EXISTS idx_contacts_contact_phone_number ON contacts(contact_phone_number);