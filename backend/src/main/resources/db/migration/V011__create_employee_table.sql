-- Staff accounts (agent.md §5.1 Employee, §14.1) — ROLE_KITCHEN / ROLE_ADMIN,
-- shared staff login used by the staff web app and native kitchen Android app.
CREATE TABLE security.employee (
    employee_id    UUID PRIMARY KEY,
    username       VARCHAR(100) NOT NULL UNIQUE,
    display_name   VARCHAR(200) NOT NULL,
    email          VARCHAR(320),
    password_hash  VARCHAR(200) NOT NULL,
    role           VARCHAR(20) NOT NULL,
    enabled        BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);
