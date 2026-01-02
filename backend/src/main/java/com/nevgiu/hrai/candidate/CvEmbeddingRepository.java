package com.nevgiu.hrai.candidate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CvEmbeddingRepository extends Repository<CvEmbedding, Long> {
    @Query(
            value = """
            SELECT candidate_id
            FROM cv_embeddings
            ORDER BY embedding <-> CAST(:embedding AS vector)
            LIMIT :limit
            """,
            nativeQuery = true
    )
    //embeddingLiteral must be something like: " [0.1,0.2,0.3,...] "
    List<Long> findNearestCandidateIds(
            @Param("embedding") String embeddingLiteral,
            @Param("limit") int limit
    );
}
