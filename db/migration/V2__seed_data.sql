-- V2__seed_data.sql

-- Вставка команд
INSERT INTO teams (id, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Development', '2021-05-10'),
       ('22222222-2222-2222-2222-222222222222', 'QA', '2021-05-10'),
       ('33333333-3333-3333-3333-333333333333', 'Sales', '2021-05-10'),
       ('44444444-4444-4444-4444-444444444444', 'HR', '2021-05-10');

-- Вставка сотрудников для команды Development
-- Менеджер: Jane Smith
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Jane', 'Smith',
        'jane.smith@example.com', 'Team Lead', NULL, 4,
        '11111111-1111-1111-1111-111111111111', '2020-08-15');

-- Сотрудники под управлением Jane Smith
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'John', 'Doe',
        'john.doe@example.com', 'Developer',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 0,
        '11111111-1111-1111-1111-111111111111', '2022-05-10'),
       ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'Mike', 'Brown',
        'mike.brown@example.com', 'Developer',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 0,
        '11111111-1111-1111-1111-111111111111', '2021-06-12'),
       ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaac', 'Sara', 'Connor',
        'sara.connor@example.com', 'Developer',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 0,
        '11111111-1111-1111-1111-111111111111', '2022-01-20'),
       ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaad', 'David', 'Lee',
        'david.lee@example.com', 'Developer',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 0,
        '11111111-1111-1111-1111-111111111111', '2023-03-05');

-- Вставка сотрудников для команды QA
-- Менеджер: Robert Johnson
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Robert', 'Johnson',
        'robert.johnson@example.com', 'QA Lead', NULL, 3,
        '22222222-2222-2222-2222-222222222222', '2021-02-10');

-- QA инженеры под управлением Robert Johnson
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Alice', 'Brown',
        'alice.brown@example.com', 'QA Engineer',
        'cccccccc-cccc-cccc-cccc-cccccccccccc', 0,
        '22222222-2222-2222-2222-222222222222', '2022-09-15'),
       ('dddddddd-dddd-dddd-dddd-ddddddddddde', 'Eve', 'Davis',
        'eve.davis@example.com', 'QA Engineer',
        'cccccccc-cccc-cccc-cccc-cccccccccccc', 0,
        '22222222-2222-2222-2222-222222222222', '2023-01-22'),
       ('dddddddd-dddd-dddd-dddd-dddddddddddf', 'Frank', 'Miller',
        'frank.miller@example.com', 'QA Engineer',
        'cccccccc-cccc-cccc-cccc-cccccccccccc', 0,
        '22222222-2222-2222-2222-222222222222', '2021-11-11');

-- Вставка сотрудников для команды Sales
-- Менеджер: Bob Johnson
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Bob', 'Johnson',
        'bob.johnson@example.com', 'Sales Manager', NULL, 2,
        '33333333-3333-3333-3333-333333333333', '2019-11-03');

-- Представители продаж под управлением Bob Johnson
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Carol', 'Williams',
        'carol.williams@example.com', 'Sales Representative',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 0,
        '33333333-3333-3333-3333-333333333333', '2020-05-15'),
       ('ffffffff-ffff-ffff-ffff-fffffffffff0', 'Steve', 'Miller',
        'steve.miller@example.com', 'Sales Representative',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 0,
        '33333333-3333-3333-3333-333333333333', '2021-08-25');

-- Вставка сотрудников для команды HR
-- Менеджер: Linda Davis
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('11111111-1111-1111-1111-aaaaaaaaaaaa', 'Linda', 'Davis',
        'linda.davis@example.com', 'HR Manager', NULL, 1,
        '44444444-4444-4444-4444-444444444444', '2018-04-20');

-- HR специалист под управлением Linda Davis
INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('11111111-1111-1111-1111-aaaaaaaaaaab', 'Nancy', 'Wilson',
        'nancy.wilson@example.com', 'HR Specialist',
        '11111111-1111-1111-1111-aaaaaaaaaaaa', 0,
        '44444444-4444-4444-4444-444444444444', '2019-12-01');

-- Обновление subordinates_count для менеджеров
UPDATE employees
SET subordinates_count = 4
WHERE id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
UPDATE employees
SET subordinates_count = 3
WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc';
UPDATE employees
SET subordinates_count = 2
WHERE id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
UPDATE employees
SET subordinates_count = 1
WHERE id = '11111111-1111-1111-1111-aaaaaaaaaaaa';
