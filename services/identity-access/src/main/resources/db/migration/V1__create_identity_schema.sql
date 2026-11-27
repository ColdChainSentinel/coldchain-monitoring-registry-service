CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'SUPERVISOR', 'OPERADOR', 'CONDUCTOR'))
);

CREATE TABLE user_point_permissions (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    monitoring_point_id UUID NOT NULL,
    PRIMARY KEY (user_id, monitoring_point_id)
);

CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_sessions_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
