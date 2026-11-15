CREATE TABLE markov_chain_scale (
    scale NUMERIC(15, 6) NOT NULL
);

CREATE UNIQUE INDEX markov_chain_scale_single_row ON markov_chain_scale ((true));

INSERT INTO markov_chain_scale (scale) VALUES (0.35);
