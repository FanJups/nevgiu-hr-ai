# CV Evaluation

## Objective

Parse candidate CVs and produce explainable, job-specific evaluation metrics that support—not replace—human review.

## Core metrics

### Job fit

- **Skills match (0–100%)**: semantic and exact overlap between required skills and extracted CV skills. The source suggests above 70% as a positive signal.
- **Experience relevance (0–10)**: relevant roles, industries, duration, and recency.
- **Education fit (0–10)**: degree level, relevant field, and certifications relative to job requirements.

### CV quality

- **Achievement impact (0–10)**: relevant, measurable outcomes and evidence-backed accomplishments.
- **Keyword density (0–100%)**: appropriate use of job-related terms while detecting keyword stuffing.

### Risk and confidence

- **Employment gap score (0–10)**: identifies gaps longer than the agreed threshold and routes them for human review. It must not infer a negative reason.
- **Readability and structure (0–10)**: clarity, organization, and appropriate length.
- **AI confidence (0–100%)**: reliability of extraction and scoring. The source suggests human verification below 80%.

### Composite score

The source proposes an overall candidate-fit score from 0–100 with these category weights:

- 40% skills and experience
- 30% education and achievements
- 30% quality and risk

This formula is not mathematically complete because individual metric weights and the treatment of AI confidence are unspecified. Finalize it before implementation; see [Open decisions](./07-open-decisions.md).

## Implementation steps

1. Define supported upload formats, file-size limits, and malware-scanning requirements.
2. Store the original CV securely and create a candidate record.
3. Extract text and document metadata, preserving page or section references where possible.
4. Normalize employment dates, roles, education, certifications, skills, and achievements.
5. Detect missing or uncertain fields and attach confidence values.
6. Load the approved job requirements and configurable metric weights.
7. Calculate each metric deterministically where possible; use AI only where semantic judgment is needed.
8. Store every score with its explanation, supporting evidence, model/prompt version, and weights.
9. Calculate the composite score only after validating that weights total 100%.
10. Flag low-confidence extraction, unexplained gaps, and parsing errors for human review.
11. Expose candidate detail, evaluation, ranking, and re-evaluation APIs.
12. Build UI views for metric breakdowns, evidence, confidence, and manual review.
13. Add bias, consistency, adversarial-CV, and regression test datasets.
14. Add a process for correcting parsed data and recalculating evaluations.

## Acceptance criteria

- The system rejects unsupported or unsafe files with a clear message.
- Every metric includes an explanation and traceable CV/job evidence.
- Users can customize permitted weights per job, and invalid totals are rejected.
- Low-confidence results are visibly marked and never presented as certain.
- Employment gaps trigger review rather than automatic rejection.
- Re-evaluation preserves an audit trail of prior results.
- Protected characteristics are excluded from scoring.
