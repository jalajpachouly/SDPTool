-- Start transaction to ensure all or none of the operations are executed
BEGIN TRANSACTION;

-- Delete records from tables that have foreign keys pointing to them first
DELETE FROM "COMMENTS";
DELETE FROM "LABEL";
DELETE FROM "PACKAGES";
DELETE FROM "CODE";
DELETE FROM "CODE_DETAILS";

-- Delete records from the ISSUE table which is referenced by other tables
DELETE FROM "ISSUE";

-- Now delete records from tables that do not have dependent foreign key constraints
DELETE FROM "AUTHORS";
DELETE FROM "CLASSES";

-- Delete records from tables that are not dependent on other tables
DELETE FROM "CONFIGURATION";

-- Commit the transaction to finalize the deletions
COMMIT;