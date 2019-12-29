CREATE TABLE migrations (
    name varchar primary key,
    insert_date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result BLOB NOT NULL
);