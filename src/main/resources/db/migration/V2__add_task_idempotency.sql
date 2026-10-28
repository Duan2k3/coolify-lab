ALTER TABLE tasks ADD COLUMN idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX uk_tasks_idempotency_key
    ON tasks (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
