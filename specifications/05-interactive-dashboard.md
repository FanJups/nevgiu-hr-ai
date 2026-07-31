# Interactive Dashboard

## Objective

Give recruiters and hiring managers a centralized view of recruitment activity, candidate evaluations, and actionable AI-assisted insights.

## Required components

### CV evaluation

- Score distribution
- Top-ranked candidates
- Low-confidence or flagged profiles
- Drill-down to candidate and evaluation evidence

### Job postings

- Active roles
- Application counts
- Time-to-fill metrics
- Drill-down to job and candidate pipeline

### Filters

- Job role
- Recruiter
- Department
- Date range

### Insights

- AI-generated observations and recommendations
- Clear links to the underlying jobs, candidates, and metrics
- Visible confidence and limitations

## Implementation steps

1. Define each metric precisely, including time zone, date boundaries, status rules, and data source.
2. Define dashboard API response models separate from persistence entities.
3. Add optimized aggregate queries and database indexes.
4. Implement job, recruiter, department, and date filters consistently across widgets.
5. Add summary cards, score distributions, ranked candidates, flagged profiles, and job overview widgets.
6. Add drill-down navigation to candidate and job detail pages.
7. Choose a near-real-time strategy: polling, server-sent events, or WebSockets.
8. Add loading, empty, partial-data, stale-data, and failure states.
9. Ensure AI insights are derived only from authorized dashboard data and link to supporting metrics.
10. Test query performance with production-like data volumes.
11. Add accessibility testing for keyboard navigation, chart labels, contrast, and screen readers.
12. Measure whether users can find key metrics without training.

## Acceptance criteria

- Initial dashboard load completes in under 3 seconds at the agreed data volume and percentile.
- Filters update all relevant widgets consistently.
- New saved data appears within the agreed freshness window.
- Every chart has a readable non-visual alternative.
- Users can drill from aggregate metrics to the underlying records.
- AI insights state their evidence and do not invent explanations.
- Target: 95% of representative users can locate agreed key metrics without training.
