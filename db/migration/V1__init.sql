-- V1__init.sql

-- Создание таблицы сотрудников
CREATE TABLE employees (
                           id UUID PRIMARY KEY,
                           first_name VARCHAR(50) NOT NULL,
                           last_name VARCHAR(50) NOT NULL,
                           email VARCHAR(100) NOT NULL UNIQUE,
                           position VARCHAR(50) NOT NULL,
                           supervisor_id UUID,
                           CONSTRAINT fk_supervisor
                               FOREIGN KEY (supervisor_id)
                                   REFERENCES employees(id)
                                   ON DELETE SET NULL
);

CREATE INDEX idx_supervisor_id ON employees(supervisor_id);

CREATE TABLE performance_reviews (
                                     id UUID PRIMARY KEY,
                                     employee_id UUID NOT NULL,
                                     review_date DATE NOT NULL,
                                     performance INTEGER CHECK (performance BETWEEN 1 AND 10),
                                     soft_skills INTEGER CHECK (soft_skills BETWEEN 1 AND 10),
                                     independence INTEGER CHECK (independence BETWEEN 1 AND 10),
                                     aspiration_for_growth INTEGER CHECK (aspiration_for_growth BETWEEN 1 AND 10),
                                     CONSTRAINT fk_employee_review
                                         FOREIGN KEY (employee_id)
                                             REFERENCES employees(id)
                                             ON DELETE CASCADE
);

CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               employee_id UUID NOT NULL,
                               notification_type VARCHAR(50) NOT NULL,
                               message TEXT NOT NULL,
                               status VARCHAR(20) DEFAULT 'new',
                               created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_employee_notification
                                   FOREIGN KEY (employee_id)
                                       REFERENCES employees(id)
                                       ON DELETE CASCADE
);

CREATE TABLE outbox (
                        id UUID PRIMARY KEY,
                        event_type VARCHAR(50) NOT NULL,
                        payload TEXT NOT NULL,
                        processed BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
