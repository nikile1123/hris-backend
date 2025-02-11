-- V1__init.sql

CREATE TABLE teams
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employees
(
    id                 UUID PRIMARY KEY,
    first_name         VARCHAR(50)  NOT NULL,
    last_name          VARCHAR(50)  NOT NULL,
    email              VARCHAR(100) NOT NULL UNIQUE,
    position           VARCHAR(50)  NOT NULL,
    supervisor_id      UUID,
    subordinates_count INTEGER      NOT NULL DEFAULT 0,
    team_id            UUID         NOT NULL,
    CONSTRAINT fk_supervisor
        FOREIGN KEY (supervisor_id)
            REFERENCES employees (id)
            ON DELETE SET NULL,
    CONSTRAINT fk_team
        FOREIGN KEY (team_id)
            REFERENCES teams (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_supervisor_id ON employees (supervisor_id);

CREATE TABLE performance_reviews
(
    id                    UUID PRIMARY KEY,
    employee_id           UUID NOT NULL,
    review_date           DATE NOT NULL,
    performance           INTEGER CHECK (performance BETWEEN 1 AND 10),
    soft_skills           INTEGER CHECK (soft_skills BETWEEN 1 AND 10),
    independence          INTEGER CHECK (independence BETWEEN 1 AND 10),
    aspiration_for_growth INTEGER CHECK (aspiration_for_growth BETWEEN 1 AND 10),
    team_id            UUID         NOT NULL,
    CONSTRAINT fk_employee_review
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_team
        FOREIGN KEY (team_id)
            REFERENCES teams (id)
            ON DELETE RESTRICT
);

CREATE TABLE outbox
(
    id          UUID PRIMARY KEY,
    employee_id UUID         NOT NULL,
    team_id     UUID         NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    message     VARCHAR(512) NOT NULL,
    processed   BOOLEAN     DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
