import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobPreviewComponent } from './job-preview.component';
import { By } from '@angular/platform-browser';
import { MOCK_JOB_GENERATION_RESPONSE } from '../../mocks/job-data';

describe('JobPreviewComponent', () => {
  let component: JobPreviewComponent;
  let fixture: ComponentFixture<JobPreviewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobPreviewComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(JobPreviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders generated job offer when jobOffer provided and not loading', () => {
    component.jobOffer = MOCK_JOB_GENERATION_RESPONSE;
    component.isLoading = false;
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    // header title and level
    expect(el.textContent).toContain(MOCK_JOB_GENERATION_RESPONSE.jobOffer.inferredTitle);
    expect(el.textContent).toContain(MOCK_JOB_GENERATION_RESPONSE.jobOffer.level);

    // responsibilities and summary should be shown
    const firstResponsibility = MOCK_JOB_GENERATION_RESPONSE.jobOffer.responsibilities[0];
    expect(el.textContent).toContain(firstResponsibility);
    expect(el.textContent).toContain(MOCK_JOB_GENERATION_RESPONSE.jobOffer.summary);

    // missing info suggestions present
    MOCK_JOB_GENERATION_RESPONSE.missingInfo.forEach(info => {
      expect(el.textContent).toContain(info);
    });
  });

  it('emits regenerate event when Regenerate button clicked', () => {
    component.jobOffer = MOCK_JOB_GENERATION_RESPONSE;
    component.isLoading = false;
    fixture.detectChanges();

    const emitSpy = spyOn(component.regenerateOffer, 'emit');

    const btn = fixture.debugElement.query(By.css('#regenerate-button'));
    expect(btn).toBeTruthy();
    btn.nativeElement.click();

    expect(emitSpy).toHaveBeenCalled();
  });

  it('shows loading spinner when isLoading is true', () => {
    component.jobOffer = null;
    component.isLoading = true;
    fixture.detectChanges();

    // Using NO_ERRORS_SCHEMA so app-loading-spinner is not declared; ensure its element exists
    const spinner = fixture.debugElement.query(By.css('app-loading-spinner'));
    expect(spinner).toBeTruthy();

    // ensure empty-state and preview are not displayed
    expect(fixture.debugElement.query(By.css('.job-preview-card'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.empty-state'))).toBeNull();
  });

  it('shows empty state when no jobOffer and not loading', () => {
    component.jobOffer = null;
    component.isLoading = false;
    fixture.detectChanges();

    const empty = fixture.debugElement.query(By.css('.empty-state'));
    expect(empty).toBeTruthy();
    expect(empty.nativeElement.textContent).toContain('No Job Offer Generated');
  });
});
