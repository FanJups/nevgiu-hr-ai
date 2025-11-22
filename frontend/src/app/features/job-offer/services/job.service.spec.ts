import { TestBed } from '@angular/core/testing';

import { JobService } from './job.service';
import { HttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { MOCK_JOB_GENERATION_RESPONSE, MOCK_JOB_GENERATION_REQUEST } from '../mocks/job-data';

describe('JobService', () => {
  let service: JobService;
  let httpSpy: jasmine.SpyObj<HttpClient>;

  beforeEach(() => {
    httpSpy = jasmine.createSpyObj('HttpClient', ['post']);
    TestBed.configureTestingModule({
      providers: [
        JobService,
        { provide: HttpClient, useValue: httpSpy }
      ]
    });
    service = TestBed.inject(JobService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('generateJobOffer calls http.post and returns response', (done) => {
    httpSpy.post.and.returnValue(of(MOCK_JOB_GENERATION_RESPONSE));

    service.generateJobOffer(MOCK_JOB_GENERATION_REQUEST).subscribe({
      next: (res) => {
        expect(res).toEqual(MOCK_JOB_GENERATION_RESPONSE);
        // ensure http.post was called (url asserted loosely to avoid hardcoding)
        expect(httpSpy.post).toHaveBeenCalledWith(jasmine.any(String), MOCK_JOB_GENERATION_REQUEST);
        done();
      },
      error: done.fail
    });
  });

  it('generateJobOffer propagates http errors', (done) => {
    const err = new Error('Network error');
    httpSpy.post.and.returnValue(throwError(() => err));

    service.generateJobOffer(MOCK_JOB_GENERATION_REQUEST).subscribe({
      next: () => done.fail('expected error'),
      error: (error) => {
        expect(error).toBe(err);
        expect(httpSpy.post).toHaveBeenCalled();
        done();
      }
    });
  });
});
