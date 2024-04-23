CREATE TABLE "AUTHORS" (
	"ID"	INTEGER,
	"ISSUE_ID"	INTEGER,
	"NAME"	TEXT,
	PRIMARY KEY("ID" AUTOINCREMENT)
)

CREATE TABLE "CLASSES" (
	"ID"	INTEGER,
	"NAME"	TEXT,
	"ISSUE_ID"	INTEGER,
	PRIMARY KEY("ID" AUTOINCREMENT)
)

CREATE TABLE "CODE" (
	"ISSUE_ID"	INTEGER,
	"CHANGES"	TEXT,
	FOREIGN KEY("ISSUE_ID") REFERENCES "ISSUE"("ISSUE_ID")
)

CREATE TABLE "CODE_DETAILS" (
	"ISSUE_ID"	INTEGER,
	"NO_OF_CLASSES"	INTEGER,
	"NO_OF_LINES"	INTEGER,
	"NO_OF_AUTHORS"	INTEGER
, "COMPLEXITY"	INTEGER)


CREATE TABLE "COMMENTS" (
	"ISSUE_ID"	INTEGER,
	"COMMENT"	TEXT,
	"PROCESSED_COMMENTS"	TEXT,
	"ID"	INTEGER,
	FOREIGN KEY("ISSUE_ID") REFERENCES "ISSUE"("ISSUE_ID"),
	PRIMARY KEY("ID")
)

CREATE TABLE "CONFIGURATION" (
	"DBLOC"	TEXT,
	"USE_SSL"	TEXT,
	"IGNORE_CERT"	TEXT,
	"ACCESS_TOKEN"	TEXT,
	"REPONAME"	TEXT,
	"RECORD_FROM"	INTEGER,
	"RECORD_TO"	INTEGER
)
CREATE TABLE "ISSUE" (
	"TITLE"	TEXT,
	"REPORTER"	TEXT,
	"OPEN_DATE"	TEXT,
	"CLOSE_DATE"	TEXT,
	"BODY"	TEXT,
	"ISSUE_ID"	INTEGER NOT NULL,
	"CLOSED_BY"	TEXT,
	"TIME_TAKEN"	NUMERIC,
	"PROCESSED_TITLES"	TEXT COLLATE BINARY,
	"PROCESSED_BODY"	TEXT,
	PRIMARY KEY("ISSUE_ID")
)
CREATE TABLE "LABEL" (
	"ISSUE_ID"	INTEGER,
	"NAME"	TEXT,
	"COLOR"	TEXT,
	FOREIGN KEY("ISSUE_ID") REFERENCES "ISSUE"("ISSUE_ID")
)
CREATE TABLE "PACKAGES" (
	"ISSUE_ID"	INTEGER,
	"ID"	INTEGER,
	"NAME"	TEXT,
	PRIMARY KEY("ID" AUTOINCREMENT)
)