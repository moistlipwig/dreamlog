# Database Migration

Create a new Flyway database migration with best practices.

## Instructions

1. **Determine Version Number**
  - List existing migrations: `ls backend/src/main/resources/db/migration/`
  - Calculate next version (highest + 1)

2. **Ask User for Details**
  - Migration description (short, snake_case)
  - What changes are needed (create table, add column, etc.)
  - Whether to include sample data

3. **Create Migration File**
  - Location: `backend/src/main/resources/db/migration/`
  - Naming: `V<N>__<description>.sql`
  - Example: `V5__add_mood_tracking.sql`

4. **Migration Template**
   Include in migration:
  - Descriptive comments
  - CREATE TABLE statements with constraints
  - Indexes for foreign keys and search columns
  - Rollback instructions in comments
  - Timestamp columns (created_at, updated_at)

5. **Update JPA Entity**
  - Check if entity exists: `ls backend/src/main/java/pl/kalin/dreamlog/model/`
  - Create or update entity class
  - Add proper JPA annotations
  - Use Lombok for getters/setters

6. **Test Migration**
  - Stop backend if running
  - Clean database: `docker compose down -v && docker compose up -d db`
  - Run backend: `./gradlew :backend:bootRun`
  - Check logs for Flyway success message
  - Verify table structure: `docker compose exec db psql -U dreamlog -c "\d <table_name>"`

7. **Update Repository (if needed)**
  - Create JpaRepository interface
  - Add custom queries if needed

## Best Practices

- NEVER modify existing migrations
- Include rollback strategy in comments
- Add indexes for foreign keys
- Use TEXT for long strings (not VARCHAR)
- Use BIGSERIAL for auto-increment IDs
- Add NOT NULL constraints where appropriate
- Include created_at/updated_at timestamps
