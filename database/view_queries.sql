-- PULSE: View & Inspection Queries for MySQL Workbench / CLI
USE pulse_db;

-- ============================================================
-- 1. View All Records in Each Table
-- ============================================================

-- View all technicians
SELECT technician_id, name, specialization, available, current_incident_id, resolved_count, created_at
FROM technicians
ORDER BY technician_id ASC;

-- View all incidents
SELECT incident_id, incident_type, title, description, location, priority, status,
       reported_by, assigned_technician_id, specific_data, created_at, updated_at, resolved_at
FROM incidents
ORDER BY incident_id ASC;

-- View all incident history logs
SELECT history_id, incident_id, previous_status, new_status, technician_id, remarks, changed_at
FROM incident_history
ORDER BY changed_at DESC, history_id DESC;

-- ============================================================
-- 2. Useful Filtered & Joined Queries
-- ============================================================

-- View OPEN incidents in PriorityQueue dispatch order (CRITICAL -> LOW, oldest first)
SELECT incident_id, incident_type, title, location, priority, status, reported_by, created_at
FROM incidents
WHERE status = 'OPEN'
ORDER BY FIELD(priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), created_at ASC, incident_id ASC;

-- View IN_PROGRESS incidents with assigned technician details
SELECT i.incident_id, i.incident_type, i.title, i.priority, i.location,
       t.technician_id, t.name AS technician_name, t.specialization, i.updated_at
FROM incidents i
LEFT JOIN technicians t ON i.assigned_technician_id = t.technician_id
WHERE i.status = 'IN_PROGRESS'
ORDER BY i.updated_at DESC;

-- View RESOLVED incidents
SELECT i.incident_id, i.incident_type, i.title, i.priority,
       t.name AS resolved_by, i.created_at, i.resolved_at
FROM incidents i
LEFT JOIN technicians t ON i.assigned_technician_id = t.technician_id
WHERE i.status = 'RESOLVED'
ORDER BY i.resolved_at DESC;

-- View available technicians
SELECT technician_id, name, specialization, resolved_count
FROM technicians
WHERE available = TRUE
ORDER BY specialization, technician_id;

-- View full incident audit trail with incident title and technician name
SELECT h.history_id, h.incident_id, i.title, h.previous_status, h.new_status,
       t.name AS technician_name, h.remarks, h.changed_at
FROM incident_history h
JOIN incidents i ON h.incident_id = i.incident_id
LEFT JOIN technicians t ON h.technician_id = t.technician_id
ORDER BY h.changed_at DESC, h.history_id DESC;

-- Dashboard summary counts
SELECT status, COUNT(*) AS total_count
FROM incidents
GROUP BY status;

SELECT priority, COUNT(*) AS total_count
FROM incidents
GROUP BY priority
ORDER BY FIELD(priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

-- ============================================================
-- 3. Reusable SQL Views
-- ============================================================

CREATE OR REPLACE VIEW v_active_incidents AS
SELECT i.incident_id, i.incident_type, i.title, i.location, i.priority, i.status,
       t.name AS assigned_technician, i.created_at
FROM incidents i
LEFT JOIN technicians t ON i.assigned_technician_id = t.technician_id
WHERE i.status IN ('OPEN', 'IN_PROGRESS');

CREATE OR REPLACE VIEW v_technician_workload AS
SELECT t.technician_id, t.name, t.specialization, t.available,
       t.current_incident_id, i.title AS current_incident_title, t.resolved_count
FROM technicians t
LEFT JOIN incidents i ON t.current_incident_id = i.incident_id;

CREATE OR REPLACE VIEW v_incident_audit_log AS
SELECT h.history_id, h.incident_id, i.title, h.previous_status, h.new_status,
       t.name AS technician_name, h.remarks, h.changed_at
FROM incident_history h
JOIN incidents i ON h.incident_id = i.incident_id
LEFT JOIN technicians t ON h.technician_id = t.technician_id;
