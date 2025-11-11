-- db-scheduler persistent task storage
-- Schema based on official db-scheduler documentation
-- Handles task scheduling, execution tracking, and retry logic

CREATE TABLE scheduled_tasks (
    task_name VARCHAR(255) NOT NULL,
    task_instance VARCHAR(255) NOT NULL,
    task_data BYTEA,                              -- Serialized task execution data (e.g., dreamId)
    execution_time TIMESTAMPTZ NOT NULL,          -- When task should execute
    picked BOOLEAN NOT NULL DEFAULT FALSE,        -- Whether task has been picked up by an executor
    picked_by VARCHAR(255),                       -- Identifier of executor that picked the task
    last_success TIMESTAMPTZ,                     -- Last successful execution timestamp
    last_failure TIMESTAMPTZ,                     -- Last failed execution timestamp
    consecutive_failures INT DEFAULT 0,           -- Number of consecutive failures (for retry logic)
    last_heartbeat TIMESTAMPTZ,                   -- Last heartbeat from executor (for dead executor detection)
    version BIGINT NOT NULL DEFAULT 1,            -- Optimistic locking version
    PRIMARY KEY (task_name, task_instance)
);

-- Index for efficient task polling (finds tasks ready to execute)
CREATE INDEX idx_scheduled_tasks_execution_time ON scheduled_tasks(execution_time) WHERE picked = FALSE;

-- Index for monitoring picked tasks
CREATE INDEX idx_scheduled_tasks_picked ON scheduled_tasks(picked, last_heartbeat);

-- Index for finding tasks by name (useful for debugging)
CREATE INDEX idx_scheduled_tasks_name ON scheduled_tasks(task_name);
