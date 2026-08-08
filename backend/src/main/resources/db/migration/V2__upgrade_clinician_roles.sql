-- Link all users who currently have the CLINICIAN role to also have the ORTHODONTIST role
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT ur.user_id, r_ortho.id
FROM user_roles ur
JOIN roles r_clin ON ur.role_id = r_clin.id
CROSS JOIN roles r_ortho
WHERE r_clin.name = 'CLINICIAN' AND r_ortho.name = 'ORTHODONTIST';
