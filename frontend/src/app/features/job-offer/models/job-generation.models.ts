export interface JobGenerationRequest {
    briefDescription: string;
    department?: string | null;
    location?: string | null;
    employmentType?: string | null;
    salaryRange?: string | null;
    tone?: string | null;
}