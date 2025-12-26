-- Create role table
CREATE TABLE role (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Create user table
CREATE TABLE "user" (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id)
        REFERENCES role(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Create member table
CREATE TABLE member (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_member_email ON member(email);
CREATE INDEX idx_member_created_at ON member(created_at);
CREATE INDEX idx_user_username ON "user"(username);
CREATE INDEX idx_user_role_id ON "user"(role_id);

-- Create function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for member table
CREATE TRIGGER update_member_updated_at
    BEFORE UPDATE ON member
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
