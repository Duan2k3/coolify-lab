# Coolify Lab Runbook

## API is unavailable

1. Check the deployment status and Deployment Logs in Coolify.
2. Check Runtime Logs for the `api` and `postgres` services.
3. Confirm both services are healthy.
4. From the API container terminal, run:

   ```sh
   curl --fail http://localhost:8080/actuator/health
   ```

5. Confirm the Coolify domain targets service `api` on internal port `8080`.
6. Confirm DNS points to the VPS and inbound ports `80` and `443` are open.

## API cannot connect to PostgreSQL

1. Do not replace the hostname `postgres` with `localhost`.
2. Confirm `SERVICE_USER_POSTGRES` and `SERVICE_PASSWORD_64_POSTGRES` have values in Coolify.
3. Check the PostgreSQL health and logs.
4. Confirm the persistent volume is still mounted at `/var/lib/postgresql/data`.

## Redis is unavailable

The API should continue serving task reads from PostgreSQL. `/actuator/health` reports the degraded dependency, but `/actuator/health/liveness` remains healthy.

1. Check the `redis` service health and logs.
2. Confirm the API uses hostname `redis` and internal port `6379`.
3. Restart only Redis if needed; cached values can be rebuilt from PostgreSQL.
4. Do not make Redis the source of truth for idempotency or task state.

## A deployment fails its health check

1. Open the API container terminal.
2. Run the same command used by the Compose health check.
3. Check whether Flyway failed because of a database connection or migration error.
4. Do not disable the health check just to make the deployment green.

## Rollback

Use the application's **Rollback** page in Coolify and select the last known-good image. Then run the smoke test.

An image rollback does not undo Flyway migrations or restore PostgreSQL data. Database changes must remain backward-compatible, and data recovery requires a separate tested backup.

## Backup and restore drill

Schedule PostgreSQL backups in Coolify and store at least one copy outside the VPS using S3-compatible storage. Periodically restore the backup into a disposable database and verify that `/api/tasks` can read the restored data.
