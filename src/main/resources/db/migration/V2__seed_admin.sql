INSERT INTO branch (code, name, is_main, created_at)
VALUES ('BR01', 'Main Branch', true, NOW());

INSERT INTO users (username, password_hash, role, branch_id, created_at)
VALUES (
    'superadmin',
    '$2a$10$RojsAqqVSiYq9n1iaL0v/ex8gw2P6zTgN/PoMCxvIEmfUNM6Se2OC',
    'SUPER_ADMIN',
    1,
    NOW()
);