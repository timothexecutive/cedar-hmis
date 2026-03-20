-- V2: Fix admin password hash
-- Password: Admin@Cedar2026

UPDATE users
SET password_hash = '$2a$12$rlVvIH7KTuIUP3IAPeRdSOHH.m3MVScqv4IcG3TKXEflaVfumsJH2'
WHERE email = 'admin@cedarhospital.co.ke';