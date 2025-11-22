import { GeneratedJobOffer } from "./generated-job-offer.models";

export interface JobGenerationResponse {
    jobOffer: GeneratedJobOffer;
    missingInfo: string[];
    suggestions: string[];
}