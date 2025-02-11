-- V2__seed_data.sql

INSERT INTO teams (id, name)
VALUES ('11111111-1111-1111-1111-111111111111', 'Development'),
       ('22222222-2222-2222-2222-222222222222', 'QA'),
       ('33333333-3333-3333-3333-333333333333', 'Sales'),
       ('44444444-4444-4444-4444-444444444444', 'HR');

INSERT INTO employees (id, first_name, last_name, email, position,
                       supervisor_id, subordinates_count, team_id)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'John', 'Doe',
        'john.doe@example.com', 'Developer', NULL, 0,
        '11111111-1111-1111-1111-111111111111'),
       ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Jane', 'Smith',
        'jane.smith@example.com', 'Team Lead', NULL, 1,
        '11111111-1111-1111-1111-111111111111'),
       ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Alice', 'Brown',
        'alice.brown@example.com', 'QA Engineer', NULL, 0,
        '22222222-2222-2222-2222-222222222222'),
       ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Bob', 'Johnson',
        'bob.johnson@example.com', 'Sales Manager', NULL, 2,
        '33333333-3333-3333-3333-333333333333'),
       ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'Carol', 'Davis',
        'carol.davis@example.com', 'HR Specialist', NULL, 0,
        '44444444-4444-4444-4444-444444444444');

UPDATE employees
SET supervisor_id      = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    subordinates_count = 1
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
