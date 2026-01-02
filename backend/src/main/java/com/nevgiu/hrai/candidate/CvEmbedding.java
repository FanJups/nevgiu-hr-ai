package com.nevgiu.hrai.candidate;

import jakarta.persistence.*;

@Entity
@Table(name = "cv_embeddings")
public class CvEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    // We don't map the vector column here – it's only used in native queries.
    // If you like, add a String or byte[] field mapped to "embedding".

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
}
