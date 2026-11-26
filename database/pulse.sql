-- PULSE: Priority-driven Unified Logistics & Service Engine
-- MySQL 8.0 Database Initialization Script

CREATE DATABASE IF NOT EXISTS pulse_db;
USE pulse_db;

-- 1. Technicians Table
CREATE TABLE IF NOT EXISTS technicians (
    technician_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization ENUM('LAB', 'ELECTRICAL', 'PROJECTOR', 'NETWORK') NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    current_incident_id INT NULL,
    resolved_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_technician_name_spec (name, specialization)
);

-- 2. Incidents Table
CREATE TABLE IF NOT EXISTS incidents (
    incident_id INT AUTO_INCREMENT PRIMARY KEY,
    incident_type ENUM('LAB', 'ELECTRICAL', 'PROJECTOR', 'NETWORK') NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(150) NOT NULL,
    priority ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'OPEN',
    reported_by VARCHAR(100) NOT NULL,
    assigned_technician_id INT NULL,
    specific_data VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    CONSTRAINT fk_incident_technician
        FOREIGN KEY (assigned_technician_id)
        REFERENCES technicians(technician_id)
        ON DELETE SET NULL
);

-- 3. Incident History Table (Audit Log)
CREATE TABLE IF NOT EXISTS incident_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    incident_id INT NOT NULL,
    previous_status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NULL,
    new_status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED') NOT NULL,
    technician_id INT NULL,
    remarks VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_incident
        FOREIGN KEY (incident_id)
        REFERENCES incidents(incident_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_history_technician
        FOREIGN KEY (technician_id)
        REFERENCES technicians(technician_id)
        ON DELETE SET NULL
);

-- Optional Sample Technicians for Initial Setup
INSERT IGNORE INTO technicians (name, specialization, available, resolved_count) VALUES
('Aarav Sharma', 'LAB', TRUE, 0),
('Priya Nair', 'ELECTRICAL', TRUE, 0),
('Rohan Verma', 'PROJECTOR', TRUE, 0),
('Sneha Kulkarni', 'NETWORK', TRUE, 0);
