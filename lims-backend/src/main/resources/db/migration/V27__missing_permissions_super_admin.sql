/*
  V27 - Add missing permissions and grant all to SUPER_ADMIN
*/

-- Missing permissions not covered in V2
INSERT INTO permission (code, description) VALUES
-- Calibration
('CALIBRATION_VIEW',           'View calibrations'),
('CALIBRATION_CREATE',         'Create calibration records'),
('CALIBRATION_COMPLETE',       'Complete calibration tasks'),
('CALIBRATION_LIMIT_VIEW',     'View calibration limits'),
('CALIBRATION_LIMIT_CREATE',   'Create calibration limits'),
('CALIBRATION_TASK_VIEW',      'View calibration tasks'),
('CALIBRATION_TASK_CREATE',    'Create calibration tasks'),
('CALIBRATION_TASK_COMPLETE',  'Complete calibration tasks'),
-- COA
('COA_VIEW',    'View COA records'),
('COA_EDIT',    'Edit COA records'),
('COA_SUBMIT',  'Submit COA for approval'),
('COA_ISSUE',   'Issue COA'),
-- Container
('CONTAINER_MANAGE', 'Manage containers'),
-- Document
('DOCUMENT_VIEW',    'View documents'),
('DOCUMENT_CREATE',  'Create documents'),
('DOCUMENT_EDIT',    'Edit documents'),
('DOCUMENT_REVIEW',  'Review documents'),
('DOCUMENT_APPROVE', 'Approve documents'),
('DOCUMENT_PUBLISH', 'Publish documents'),
('DOCUMENT_RETIRE',  'Retire documents'),
('DOCUMENT_SUBMIT',  'Submit documents for review'),
('DOCUMENT_MANAGE',  'Manage document versions'),
-- ELN
('ELN_DELETE', 'Delete ELN entries'),
-- Inventory extended
('INVENTORY_VIEW',    'View inventory'),
('INVENTORY_MANAGE',  'Manage inventory'),
('INVENTORY_CONSUME', 'Consume inventory items'),
-- Notifications
('NOTIFICATION_VIEW', 'View notifications'),
-- OOS/CAPA extended
('OOS_EDIT',  'Edit OOS cases'),
('OOS_CLOSE', 'Close OOS cases'),
('CAPA_EDIT', 'Edit CAPA records'),
-- Sample extended
('SAMPLE_RECEIVE',  'Receive samples'),
('SAMPLE_APPROVE',  'Approve samples'),
('SAMPLE_REJECT',   'Reject samples'),
('SAMPLE_ARCHIVE',  'Archive samples'),
('SAMPLE_RELEASE',  'Release samples'),
('SAMPLE_ADMIN',    'Full sample administration'),
('RESULT_REJECT',   'Reject test results'),
-- Storage
('STORAGE_MANAGE', 'Manage storage locations'),
-- Stability
('STABILITY_VIEW',   'View stability studies'),
('STABILITY_CREATE', 'Create stability studies'),
('STABILITY_MANAGE', 'Manage stability studies'),
-- Scheduler
('ADMIN',        'System administration access'),
('SYSTEM_ADMIN', 'Full system administration'),
-- Tasks
('TASK_EDIT',   'Edit tasks'),
('TASK_ACTION', 'Perform task actions'),
-- Worksheet / Validation
('WORKSHEET_EDIT',         'Edit worksheets'),
('WORKSHEET_SUBMIT',       'Submit worksheets for review'),
('VALIDATION_RULE_VIEW',   'View worksheet validation rules'),
('VALIDATION_RULE_MANAGE', 'Manage worksheet validation rules'),
-- Document template
('DOCUMENT_TEMPLATE_VIEW', 'View document templates'),
('DOCUMENT_VERSION_UPLOAD','Upload document versions')
ON CONFLICT (code) DO NOTHING;

-- Grant every permission to SUPER_ADMIN role for tenant_id = 1
INSERT INTO tenant_role_permission (tenant_id, role_id, permission_id)
SELECT 1, r.id, p.id
FROM role r
CROSS JOIN permission p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
