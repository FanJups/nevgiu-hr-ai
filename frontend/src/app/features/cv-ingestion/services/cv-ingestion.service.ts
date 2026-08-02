import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environments';
import { Candidate, CvArchiveImportResult, CvImportResult, EvaluationResponse, Job } from '../models/cv-ingestion.models';

@Injectable({ providedIn: 'root' })
export class CvIngestionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  uploadPdf(file: File): Observable<CvImportResult> {
    return this.http.post<CvImportResult>(`${this.apiUrl}/candidates/import`, this.formData(file));
  }

  uploadArchive(file: File): Observable<CvArchiveImportResult> {
    return this.http.post<CvArchiveImportResult>(`${this.apiUrl}/candidates/import/archive`, this.formData(file));
  }

  importInitial(): Observable<CvArchiveImportResult> {
    return this.http.post<CvArchiveImportResult>(`${this.apiUrl}/candidates/import/initial`, {});
  }

  getCandidates(): Observable<Candidate[]> {
    return this.http.get<Candidate[]>(`${this.apiUrl}/candidates`);
  }

  getJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(`${this.apiUrl}/jobs`);
  }

  evaluate(candidateId: number, jobId: number): Observable<EvaluationResponse> {
    return this.http.post<EvaluationResponse>(`${this.apiUrl}/evaluations`, { candidateId, jobId, weights: null });
  }

  private formData(file: File): FormData {
    const data = new FormData();
    data.append('file', file, file.name);
    return data;
  }
}
