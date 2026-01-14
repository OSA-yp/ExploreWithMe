CREATE TABLE IF NOT EXISTS hits (
                                    id BIGSERIAL PRIMARY KEY,
                                    app VARCHAR(255) NOT NULL,
                                    uri VARCHAR(1024) NOT NULL,
                                    ip VARCHAR(45) NOT NULL,
                                    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
