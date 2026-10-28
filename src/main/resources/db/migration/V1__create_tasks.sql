CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tasks_created_at ON tasks (created_at DESC);
CREATE INDEX idx_tasks_status ON tasks (status);
