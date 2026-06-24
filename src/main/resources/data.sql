-- Initial staff administrator.
-- Username: staff; initial password: ChangeMe123! (stored below as a BCrypt cost-12 hash).
INSERT INTO staff_users (username, password_hash, role, enabled, created_at)
SELECT 'staff', '$2a$12$NjcUE2gOH4lWTLOUnPNn2ennC9Xzr0RaLwnpKcBY/7zTCi2ooc3Qu', 'ADMIN', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM staff_users WHERE LOWER(username) = 'staff');

-- Departments
INSERT INTO departments
(code, name, prefix, daily_quota, opening_time, closing_time, break_start_time, break_end_time)
VALUES
('GEN', 'General Consultation', 'GEN', 100, '06:00:00', '22:00:00', '12:00:00', '14:00:00'),
('PHA', 'Pharmacy', 'PHA', 150, '06:00:00', '22:00:00', '12:00:00', '14:00:00'),
('DEN', 'Dental Clinic', 'DEN', 40, '06:00:00', '17:00:00', '12:00:00', '14:00:00'),
('LAB', 'Blood Test Unit', 'LAB', 80, '06:00:00', '17:00:00', '12:00:00', '14:00:00'),
('SPC', 'Specialist Clinic', 'SPC', 50, '06:00:00', '17:00:00', '12:00:00', '14:00:00');

-- Counters
INSERT INTO counters (name, department_code, status) VALUES
('Counter 1', 'GEN', 'OPEN'),
('Counter 2', 'GEN', 'OPEN'),
('Counter 3', 'PHA', 'OPEN'),
('Counter 4', 'DEN', 'OPEN'),
('Counter 5', 'LAB', 'OPEN'),
('Counter 6', 'SPC', 'OPEN');

-- Malaysian IC state codes
INSERT INTO ic_states (code, state_name) VALUES
('01', 'Johor'),
('02', 'Kedah'),
('03', 'Kelantan'),
('04', 'Melaka'),
('05', 'Negeri Sembilan'),
('06', 'Pahang'),
('07', 'Pulau Pinang'),
('08', 'Perak'),
('09', 'Perlis'),
('10', 'Selangor'),
('11', 'Terengganu'),
('12', 'Sabah'),
('13', 'Sarawak'),
('14', 'Kuala Lumpur'),
('15', 'Labuan'),
('16', 'Putrajaya');

-- Phone country codes
INSERT INTO phone_codes (country_name, iso_code, dial_code) VALUES
('Malaysia', 'MY', '+60'),
('Singapore', 'SG', '+65'),
('Indonesia', 'ID', '+62'),
('Thailand', 'TH', '+66'),
('Brunei', 'BN', '+673'),
('Philippines', 'PH', '+63'),
('Vietnam', 'VN', '+84'),
('China', 'CN', '+86'),
('India', 'IN', '+91'),
('Bangladesh', 'BD', '+880'),
('Pakistan', 'PK', '+92'),
('Nepal', 'NP', '+977'),
('Myanmar', 'MM', '+95'),
('Japan', 'JP', '+81'),
('South Korea', 'KR', '+82'),
('Australia', 'AU', '+61'),
('United Kingdom', 'GB', '+44'),
('United States', 'US', '+1'),
('Canada', 'CA', '+1');
