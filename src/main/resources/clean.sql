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
delete from PULL_RQ;
delete from pr_files_changed;
delete from pr_review_comments;
delete from pr_reviewers;