export interface GeneratedJobOffer {
    id?: number;
    title?: string;
    inferredTitle: string;
    level: string;
    summary: string;
    responsibilities: string[];
    requiredQualifications: string[];
    preferredQualifications: string[];
    softSkills: string[];
    benefits: string[];
    employmentType: string;
    location: string;
    salaryRange: string;
    tone: string;
}
