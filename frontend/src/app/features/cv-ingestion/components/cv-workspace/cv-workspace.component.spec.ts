import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, Subject, throwError } from 'rxjs';
import { Candidate, CvArchiveImportResult, CvImportResult, EvaluationResponse, Job } from '../../models/cv-ingestion.models';
import { CvIngestionService } from '../../services/cv-ingestion.service';
import { CvWorkspaceComponent } from './cv-workspace.component';

describe('CvWorkspaceComponent', () => {
  let fixture: ComponentFixture<CvWorkspaceComponent>;
  let component: CvWorkspaceComponent;
  let service: jasmine.SpyObj<CvIngestionService>;

  const candidate: Candidate = {
    id: 7, name: 'Ada Lovelace', email: 'ada@example.com', currentTitle: null, cvText: 'Engineer',
  };
  const job: Job = { id: 12, title: 'Backend Engineer', level: 'Senior', summary: 'Build APIs' };

  beforeEach(async () => {
    service = jasmine.createSpyObj<CvIngestionService>('CvIngestionService', [
      'uploadPdf', 'uploadArchive', 'importInitial', 'getCandidates', 'getJobs', 'evaluate',
    ]);
    service.getCandidates.and.returnValue(of([candidate]));
    service.getJobs.and.returnValue(of([job]));

    await TestBed.configureTestingModule({
      imports: [CvWorkspaceComponent],
      providers: [{ provide: CvIngestionService, useValue: service }],
    }).compileComponents();

    fixture = TestBed.createComponent(CvWorkspaceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads candidates and jobs and selects defaults', () => {
    expect(service.getCandidates).toHaveBeenCalled();
    expect(service.getJobs).toHaveBeenCalled();
    expect(component.selectedCandidateId).toBe(7);
    expect(component.selectedJobId).toBe(12);
  });

  it('rejects a non-PDF selection', () => {
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [new File(['x'], 'candidate.txt')] });
    component.selectPdf({ target: input } as unknown as Event);
    expect(component.pdfFile).toBeNull();
    expect(component.error).toContain('.PDF');
  });

  it('rejects an oversized ZIP selection', () => {
    const file = new File(['x'], 'candidates.zip');
    Object.defineProperty(file, 'size', { value: component.maxArchiveBytes + 1 });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    component.selectArchive({ target: input } as unknown as Event);
    expect(component.archiveFile).toBeNull();
    expect(component.error).toContain('100 MB');
  });

  it('uploads a PDF, renders its result, and refreshes candidates', () => {
    const imported = importResult('IMPORTED');
    service.uploadPdf.and.returnValue(of(imported));
    component.pdfFile = new File(['%PDF'], 'ada.pdf', { type: 'application/pdf' });
    component.uploadPdf();
    fixture.detectChanges();

    expect(service.uploadPdf).toHaveBeenCalled();
    expect(component.result?.imported).toBe(1);
    expect(service.getCandidates).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('IMPORTED');
  });

  it('disables conflicting actions while an archive request is active', () => {
    const pending = new Subject<CvArchiveImportResult>();
    service.uploadArchive.and.returnValue(pending);
    component.archiveFile = new File(['zip'], 'candidates.zip');
    component.uploadArchive();
    fixture.detectChanges();

    expect(component.busyAction).toBe('archive');
    const buttons = fixture.debugElement.queryAll(By.css('button'));
    expect(buttons.every((button) => button.nativeElement.disabled)).toBeTrue();
    pending.next(archiveResult([importResult('IMPORTED')]));
    pending.complete();
    expect(component.busyAction).toBeNull();
  });

  it('shows duplicate, review, skipped, and failed archive outcomes', () => {
    const results = ['DUPLICATE', 'NEEDS_REVIEW', 'SKIPPED', 'FAILED'].map((status) =>
      importResult(status as CvImportResult['status']));
    service.importInitial.and.returnValue(of(archiveResult(results)));
    component.importInitial();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('DUPLICATE');
    expect(text).toContain('NEEDS_REVIEW');
    expect(text).toContain('SKIPPED');
    expect(text).toContain('FAILED');
  });

  it('does not evaluate until explicitly requested', () => {
    expect(service.evaluate).not.toHaveBeenCalled();
    const response = evaluationResponse();
    service.evaluate.and.returnValue(of(response));
    component.evaluate();
    expect(service.evaluate).toHaveBeenCalledOnceWith(7, 12);
  });

  it('does not evaluate without both selections', () => {
    component.selectedCandidateId = null;
    component.evaluate();
    expect(service.evaluate).not.toHaveBeenCalled();
  });

  it('renders all evaluation metrics and explanation', () => {
    service.evaluate.and.returnValue(of(evaluationResponse()));
    component.evaluate();
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('82');
    expect(text).toContain('Skills match');
    expect(text).toContain('AI confidence');
    expect(text).toContain('Strong backend alignment');
  });

  it('clears stale evaluation and displays structured provider errors', () => {
    component.evaluation = evaluationResponse();
    service.evaluate.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 502, error: { message: 'Evaluation provider returned invalid JSON' },
    })));
    component.evaluate();
    fixture.detectChanges();
    expect(component.evaluation).toBeNull();
    expect(component.error).toBe('Evaluation provider returned invalid JSON');
  });

  function importResult(status: CvImportResult['status']): CvImportResult {
    return {
      candidateId: status === 'IMPORTED' || status === 'DUPLICATE' ? 7 : null,
      documentId: 2, originalFilename: `${status}.pdf`, status,
      contentType: 'application/pdf', textLength: 120, warnings: status === 'IMPORTED' ? [] : ['Review outcome'],
    };
  }

  function archiveResult(results: CvImportResult[]): CvArchiveImportResult {
    const count = (status: CvImportResult['status']) => results.filter((item) => item.status === status).length;
    return {
      totalFiles: results.length, imported: count('IMPORTED'), duplicates: count('DUPLICATE'),
      needsReview: count('NEEDS_REVIEW'), skipped: count('SKIPPED'), failed: count('FAILED'), results,
    };
  }

  function evaluationResponse(): EvaluationResponse {
    return {
      explanation: 'Strong backend alignment',
      evaluation: {
        id: 1, candidateId: 7, jobId: 12, skillsMatchScore: 90, experienceRelevanceScore: 8,
        educationFitScore: 7, achievementImpactScore: 8, keywordDensityScore: 75,
        employmentGapScore: 9, readabilityScore: 8, aiConfidenceScore: 88,
        overallFitScore: 82, createdAt: '2026-08-01T00:00:00Z',
      },
    };
  }
});
