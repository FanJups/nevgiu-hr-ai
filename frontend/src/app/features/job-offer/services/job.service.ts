import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobGenerationRequest } from '../models/job-generation.models';
import { environment } from '../../../../environments/environments';
import { JobGenerationResponse } from '../models/job-generation-response.models';
import { ApproveJobRequest } from '../models/approve-job-request.models';
import { GeneratedJobOffer } from '../models/generated-job-offer.models';

@Injectable()
export class JobService {

  private apiUrl = environment.apiUrl;
  constructor(private http: HttpClient) { }

  generateJobOffer(jobOffer: JobGenerationRequest): Observable<JobGenerationResponse> {
    return this.http.post<JobGenerationResponse>(`${this.apiUrl}/jobs/generate`, jobOffer);
  }

  getJobs(): Observable<GeneratedJobOffer[]> {
    return this.http.get<GeneratedJobOffer[]>(`${this.apiUrl}/jobs`);
  }

  approveJobOffer(approveRequest: ApproveJobRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/jobs/approve`, approveRequest);
  }


}
