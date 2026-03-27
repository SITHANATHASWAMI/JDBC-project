CREATE DATABASE bug_tracker_db;


USE bug_tracker_db;


IF OBJECT_ID('bugs', 'U') IS NOT NULL
DROP TABLE bugs;


CREATE TABLE bugs (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    reporter VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) DEFAULT '',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
); 
