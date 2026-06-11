-- V28 - Add is_active flag to role table + analytics permission
ALTER TABLE role ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Add ANALYTICS_VIEW permission
INSERT INTO permission (code, description)
VALUES ('ANALYTICS_VIEW', 'View analytics dashboards and chart data')
ON CONFLICT (code) DO NOTHING;

-- Re-grant all permissions to SUPER_ADMIN for tenant_id = 1
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT 1, r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
