import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environments';
import { CvIngestionService } from './cv-ingestion.service';

describe('CvIngestionService', () => {
  let service: CvIngestionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CvIngestionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('uploads a PDF as multipart form data', () => {
    const file = new File(['%PDF-test'], 'candidate.pdf', { type: 'application/pdf' });
    service.uploadPdf(file).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/candidates/import`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body instanceof FormData).toBeTrue();
    expect((request.request.body as FormData).get('file')).toBe(file);
    request.flush({ status: 'IMPORTED' });
  });

  it('submits an explicit candidate and job evaluation', () => {
    service.evaluate(7, 12).subscribe();

    const request = http.expectOne(`${environment.apiUrl}/evaluations`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ candidateId: 7, jobId: 12, weights: null });
    request.flush({ evaluation: {}, explanation: 'Good fit' });
  });
});
