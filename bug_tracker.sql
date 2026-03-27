CREATE DATABASE IF NOT EXISTS bug_tracker_db;

USE bug_tracker_db;

DROP TABLE IF EXISTS bugs;

CREATE TABLE bugs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    reporter VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) DEFAULT '',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
INSERT INTO bugs (title, description, reporter, assignee, priority, status)
VALUES ('Login Error', 'User cannot login', 'Admin', 'Developer', 'HIGH', 'OPEN');
USE bug_tracker_db;
SELECT * FROM bugs;
