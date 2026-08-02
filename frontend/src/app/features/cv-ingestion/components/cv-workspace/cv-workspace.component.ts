import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  ApiError,
  Candidate,
  CvArchiveImportResult,
  CvImportResult,
  EvaluationResponse,
  Job,
} from '../../models/cv-ingestion.models';
import { CvIngestionService } from '../../services/cv-ingestion.service';

@Component({
  selector: 'app-cv-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cv-workspace.component.html',
  styleUrl: './cv-workspace.component.scss',
})
export class CvWorkspaceComponent implements OnInit {
  private readonly service = inject(CvIngestionService);

  pdfFile: File | null = null;
  archiveFile: File | null = null;
  result: CvArchiveImportResult | null = null;
  candidates: Candidate[] = [];
  jobs: Job[] = [];
  selectedCandidateId: number | null = null;
  selectedJobId: number | null = null;
  evaluation: EvaluationResponse | null = null;
  busyAction: 'pdf' | 'archive' | 'initial' | 'evaluation' | null = null;
  error = '';

  readonly maxPdfBytes = 20 * 1024 * 1024;
  readonly maxArchiveBytes = 100 * 1024 * 1024;

  ngOnInit(): void {
    this.refreshSelections();
  }

  selectPdf(event: Event): void {
    this.pdfFile = this.selectedFile(event, '.pdf', this.maxPdfBytes);
  }

  selectArchive(event: Event): void {
    this.archiveFile = this.selectedFile(event, '.zip', this.maxArchiveBytes);
  }

  uploadPdf(): void {
    if (!this.pdfFile || this.busyAction) return;
    this.start('pdf');
    this.service.uploadPdf(this.pdfFile).pipe(finalize(() => this.busyAction = null)).subscribe({
      next: (item) => {
        this.result = this.asArchiveResult(item);
        this.pdfFile = null;
        this.refreshSelections(item.candidateId);
      },
      error: (error) => this.handleError(error),
    });
  }

  uploadArchive(): void {
    if (!this.archiveFile || this.busyAction) return;
    this.start('archive');
    this.service.uploadArchive(this.archiveFile).pipe(finalize(() => this.busyAction = null)).subscribe({
      next: (result) => {
        this.result = result;
        this.archiveFile = null;
        this.refreshSelections(this.firstCandidateId(result));
      },
      error: (error) => this.handleError(error),
    });
  }

  importInitial(): void {
    if (this.busyAction) return;
    this.start('initial');
    this.service.importInitial().pipe(finalize(() => this.busyAction = null)).subscribe({
      next: (result) => {
        this.result = result;
        this.refreshSelections(this.firstCandidateId(result));
      },
      error: (error) => this.handleError(error),
    });
  }

  evaluate(): void {
    if (!this.selectedCandidateId || !this.selectedJobId || this.busyAction) return;
    this.start('evaluation');
    this.evaluation = null;
    this.service.evaluate(this.selectedCandidateId, this.selectedJobId)
      .pipe(finalize(() => this.busyAction = null))
      .subscribe({
        next: (result) => this.evaluation = result,
        error: (error) => this.handleError(error),
      });
  }

  trackResult(_: number, item: CvImportResult): string {
    return `${item.originalFilename}-${item.documentId ?? item.status}`;
  }

  private refreshSelections(preferredCandidateId?: number | null): void {
    this.service.getCandidates().subscribe({
      next: (candidates) => {
        this.candidates = candidates;
        if (preferredCandidateId && candidates.some((item) => item.id === preferredCandidateId)) {
          this.selectedCandidateId = preferredCandidateId;
        } else if (!this.selectedCandidateId && candidates.length) {
          this.selectedCandidateId = candidates[0].id;
        }
      },
      error: (error) => this.handleError(error),
    });
    this.service.getJobs().subscribe({
      next: (jobs) => {
        this.jobs = jobs;
        if (!this.selectedJobId && jobs.length) this.selectedJobId = jobs[0].id;
      },
      error: (error) => this.handleError(error),
    });
  }

  private selectedFile(event: Event, extension: string, limit: number): File | null {
    this.error = '';
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) return null;
    if (!file.name.toLowerCase().endsWith(extension)) {
      this.error = `Choose a ${extension.toUpperCase()} file.`;
      input.value = '';
      return null;
    }
    if (file.size > limit) {
      this.error = `${file.name} exceeds the ${Math.round(limit / 1024 / 1024)} MB limit.`;
      input.value = '';
      return null;
    }
    return file;
  }

  private start(action: typeof this.busyAction): void {
    this.error = '';
    this.evaluation = action === 'evaluation' ? null : this.evaluation;
    this.busyAction = action;
  }

  private handleError(error: HttpErrorResponse): void {
    const apiError = error.error as Partial<ApiError> | undefined;
    this.error = apiError?.message || error.message || 'The request could not be completed.';
  }

  private firstCandidateId(result: CvArchiveImportResult): number | null {
    return result.results.find((item) => item.candidateId !== null)?.candidateId ?? null;
  }

  private asArchiveResult(item: CvImportResult): CvArchiveImportResult {
    return {
      totalFiles: 1,
      imported: item.status === 'IMPORTED' ? 1 : 0,
      duplicates: item.status === 'DUPLICATE' ? 1 : 0,
      needsReview: item.status === 'NEEDS_REVIEW' ? 1 : 0,
      skipped: item.status === 'SKIPPED' ? 1 : 0,
      failed: item.status === 'FAILED' ? 1 : 0,
      results: [item],
    };
  }
}
