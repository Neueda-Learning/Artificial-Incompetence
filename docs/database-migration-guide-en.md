# Database Table Structure Modification Guide

This document specifies the unified workflow for team members when modifying MySQL table structures.

## 1. Core Principles

- Database table structures are managed uniformly through Flyway migration files.
- Do not manually modify table structures only in your personal MySQL instance.
- Every structural change must create a new migration file.
- Migration files that have already been committed or executed by other team members must not be modified, deleted, or renamed.
- Migration files, corresponding Java Entities, DTOs, Repositories, and tests should be placed in the same branch or Pull Request whenever possible.
- Sensitive information such as passwords should only reside in the local `.env` file and must not be written into migration files or committed to Git.

Migration file directory:

```text
backend/src/main/resources/db/migration/
```

Current migrations:

```text
V1__create_portfolio_items.sql
V2__create_transactions.sql
```

## 2. Before Making Changes

### Step 1: Describe the Change Requirements

Team members should first explain in a group chat, Issue, or Pull Request:

- Which table needs to be modified.
- What fields are being added, modified, or removed.
- The reason for the change.
- Whether existing data will be affected.
- Whether corresponding interface and Java code changes are needed.

### Step 2: Update Local Code

Switch to your development branch, then pull the latest code from the shared branch:

```bash
git switch <your-branch-name>
git fetch origin
git merge origin/main
```

If the team's shared branch is not `main`, replace `main` in the command above with the actual branch name.

### Step 3: Determine the Next Version Number

Check the migration directory:

```bash
ls backend/src/main/resources/db/migration
```

If the current latest version is `V2`, the next migration should use `V3`.

The same version number cannot be used for two files simultaneously. Confirm the version number within the team before creating the file to prevent two members from independently creating different `V3` files.

## 3. Creating Migration Files

File name format:

```text
V<version-number>__<english-description>.sql
```

Use two underscores between the version number and the description.

For example, adding a notes field to the `transactions` table:

```text
V3__add_notes_to_transactions.sql
```

File content:

```sql
ALTER TABLE transactions
    ADD COLUMN notes VARCHAR(255) NULL;
```

Other examples:

```sql
-- Create an index
CREATE INDEX idx_portfolio_items_symbol
    ON portfolio_items (symbol);

-- Provide a default value for existing records when adding a non-null column
ALTER TABLE transactions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';

-- Modify column length
ALTER TABLE portfolio_items
    MODIFY COLUMN symbol VARCHAR(30) NOT NULL;
```

## 4. Synchronizing Spring Boot Code Changes

After database fields have changed, check whether the following need to be updated:

- JPA Entity and its `@Column` configuration.
- Request and Response DTOs.
- Repository queries.
- Service business logic and `@Transactional` scope.
- Controller interfaces.
- Unit tests and integration tests.

For example, if a `notes` field is added to the database, the corresponding Entity can be updated with:

```java
@Column(length = 255)
private String notes;
```

The Java field type, length, nullability, and precision should be consistent with the migration file. The project's Hibernate configuration uses `ddl-auto: validate`, which checks at startup whether the Entity and database structure are compatible, but it will not automatically modify the table structure on behalf of the team.

## 5. Local Verification

Build and start the services:

```bash
docker compose up --build
```

Automatic execution order at startup:

```text
MySQL starts
    ↓
Spring Boot starts
    ↓
Flyway reads flyway_schema_history
    ↓
Executes new migrations that have not yet run
    ↓
Hibernate validates the table structure
    ↓
Backend begins serving API
```

Check container status:

```bash
docker compose ps
```

Access MySQL:

```bash
docker compose exec database \
  mysql -uportfolio_user -p portfolio
```

If your personal `.env` uses a different database name or username, replace `portfolio_user` and `portfolio` in the command accordingly.

Check the table structure and migration records:

```sql
SHOW TABLES;
DESCRIBE transactions;
SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Then run the backend tests:

```bash
cd backend
mvn test
```

Verification requirements:

- Spring Boot starts normally.
- The new migration has `success` = `1` in `flyway_schema_history`.
- The table structure is consistent with the Java Entity.
- Existing APIs still work correctly.
- New feature tests pass.

## 6. Committing and Team Synchronization

After confirming that verification passes, commit:

```bash
git status
git add backend/src/main/resources/db/migration
git add <other-related-code-and-tests>
git commit -m "Add notes to transactions"
git push
```

After the migration is merged into the shared branch, other team members should run:

```bash
git pull
docker compose up --build
```

Flyway will only execute migrations that have not yet been run in that member's database and will not re-execute versions that have already succeeded.

## 7. Handling Conflicts and Failures

### Two People Used the Same Version Number

If two branches both created a `V3`, the member who merges later should rename their file to the next available version before merging, for example `V4`.

If the original migration was only executed in that member's personal database, after renaming, they need to rebuild their personal development database, or handle the local migration records after confirming there is no shared impact. Do not modify other members' databases.

### Migration Has Not Yet Been Shared

If the erroneous migration has not yet been pushed and has only been executed on the creator's local environment, the file can be corrected and the creator's own local database can be rebuilt.

Clearing the volume will permanently delete the local database. Only the member themselves should perform this, and only after confirming the data is not needed or has been backed up:

```bash
docker compose down -v
docker compose up --build
```

### Migration Has Already Been Executed by Other Members

Do not modify the original migration. Create a new migration to correct it:

```text
V4__fix_transaction_notes.sql
```

For example:

```sql
ALTER TABLE transactions
    MODIFY COLUMN notes VARCHAR(500) NULL;
```

### Need to Drop Fields or Tables

Dropping fields, dropping tables, shortening field lengths, and changing data types can all cause data loss. Before executing, you must:

1. Notify all team members.
2. Confirm that the code no longer uses the target field or table.
3. Check and back up any data that needs to be preserved.
4. Verify the migration on your personal development database.
5. Merge only after passing code review.

## 8. Prohibited Actions

Team members must not:

- Use database tools to directly modify tables without committing a migration.
- Modify historical files such as `V1` or `V2` that have already been shared or executed.
- Delete `flyway_schema_history`.
- Arbitrarily run Flyway repair to cover up migration validation errors.
- Write production data or personal passwords into SQL migrations.
- Execute `docker compose down -v` without confirmation.
- Manually forge a successful record after a migration failure.

## 9. Workflow Summary

```text
Propose a structural change requirement
    ↓
Pull the latest code from the shared branch
    ↓
Team confirms the next migration version number
    ↓
Create a new Flyway migration
    ↓
Synchronize changes to Entity, DTO, Repository, and tests
    ↓
Start MySQL and Spring Boot locally
    ↓
Check table structure and flyway_schema_history
    ↓
Run tests
    ↓
Commit and merge
    ↓
Other members pull the code and auto-migrate
```
