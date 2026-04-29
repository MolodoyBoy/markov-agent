CREATE TABLE markov_chain (
    from_state INT NOT NULL,
    to_state INT NOT NULL,
    probability NUMERIC(15, 6) NOT NULL
);