CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE cv_embeddings (
                               id BIGSERIAL PRIMARY KEY,
                               candidate_id BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
                               embedding vector(1536) NOT NULL  -- adjust to your embedding dimension
);
