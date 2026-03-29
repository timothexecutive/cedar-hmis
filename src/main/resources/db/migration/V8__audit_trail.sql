-- V8: Audit Trail

CREATE SEQUENCE IF NOT EXISTS audit_trail_SEQ start 1 increment 50;

CREATE TABLE audit_trail (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    user_name   VARCHAR(200),
    user_role   VARCHAR(50),
    action      VARCHAR(100) NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    resource    VARCHAR(100),
    resource_id VARCHAR(100),
    old_value   TEXT,
    new_value   TEXT,
    description TEXT,
    ip_address  VARCHAR(50),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_trail_user     ON audit_trail(user_id);
CREATE INDEX idx_audit_trail_action   ON audit_trail(action);
CREATE INDEX idx_audit_trail_module   ON audit_trail(module);
CREATE INDEX idx_audit_trail_created  ON audit_trail(created_at);
CREATE INDEX idx_audit_trail_resource ON audit_trail(resource, resource_id);