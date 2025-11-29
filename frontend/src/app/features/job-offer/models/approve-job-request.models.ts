import { GeneratedJobOffer } from './generated-job-offer.models';
import { JobGenerationRequest } from "./job-generation.models";

export interface ApproveJobRequest {
    originalRequest: JobGenerationRequest;
    finalJobOffer: GeneratedJobOffer;
}
