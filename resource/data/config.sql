CREATE TABLE IF NOT EXISTS code_snippets (
    language varchar(16) not null,
    technology varchar(32),
    title varchar(64) not null,
    description text,
    snippettype varchar(8),
    source_file varchar(32), -- UUID without hyphens is 32 characters
    id integer not null primary key AUTOINCREMENT
);
CREATE TABLE IF NOT EXISTS categories (
    language varchar(16) not null,
    technology varchar(32),
    snippettype varchar(8),
    id integer not null primary key AUTOINCREMENT
);