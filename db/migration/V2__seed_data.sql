-- V2__seed_data.sql

INSERT INTO teams (id, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Development', '2021-05-10'),
       ('22222222-2222-2222-2222-222222222222', 'QA', '2021-05-10'),
       ('33333333-3333-3333-3333-333333333333', 'Sales', '2021-05-10'),
       ('44444444-4444-4444-4444-444444444444', 'HR', '2021-05-10');

INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id, joining_date)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'John', 'Doe',
        'john.doe@example.com', 'Developer', NULL, 0,
        '11111111-1111-1111-1111-111111111111', '2022-05-10'),
       ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Jane', 'Smith',
        'jane.smith@example.com', 'Team Lead', NULL, 1,
        '11111111-1111-1111-1111-111111111111', '2020-08-15'),
       ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Alice', 'Brown',
        'alice.brown@example.com', 'QA Engineer', NULL, 0,
        '22222222-2222-2222-2222-222222222222', '2023-02-20'),
       ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Bob', 'Johnson',
        'bob.johnson@example.com', 'Sales Manager', NULL, 2,
        '33333333-3333-3333-3333-333333333333', '2019-11-03'),
       ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Carol', 'Davis',
        'carol.davis@example.com', 'HR Specialist', NULL, 0,
        '44444444-4444-4444-4444-444444444444', '2021-07-12');

UPDATE employees
SET supervisor_id      = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    subordinates_count = 1
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
