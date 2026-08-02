export type CvIngestionStatus = 'IMPORTED' | 'DUPLICATE' | 'NEEDS_REVIEW' | 'SKIPPED' | 'FAILED';

export interface CvImportResult {
  candidateId: number | null;
  documentId: number | null;
  originalFilename: string;
  status: CvIngestionStatus;
  contentType: string | null;
  textLength: number;
  warnings: string[];
}

export interface CvArchiveImportResult {
  totalFiles: number;
  imported: number;
  duplicates: number;
  needsReview: number;
  skipped: number;
  failed: number;
  results: CvImportResult[];
}

export interface Candidate {
  id: number;
  name: string;
  email: string | null;
  currentTitle: string | null;
  cvText: string;
}

export interface Job {
  id: number;
  title: string;
  level: string;
  summary: string;
}

export interface CandidateEvaluation {
  id: number;
  candidateId: number;
  jobId: number;
  skillsMatchScore: number;
  experienceRelevanceScore: number;
  educationFitScore: number;
  achievementImpactScore: number;
  keywordDensityScore: number;
  employmentGapScore: number;
  readabilityScore: number;
  aiConfidenceScore: number;
  overallFitScore: number;
  createdAt: string;
}

export interface EvaluationResponse {
  evaluation: CandidateEvaluation;
  explanation: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
}
