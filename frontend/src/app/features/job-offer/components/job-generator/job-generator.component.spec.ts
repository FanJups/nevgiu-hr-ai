import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobGeneratorComponent } from './job-generator.component';
import { JobService } from '../../services/job.service';
import { MOCK_JOB_GENERATION_REQUEST, MOCK_JOB_GENERATION_RESPONSE } from '../../mocks/job-data';
import { of, throwError } from 'rxjs';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { JobGenerationRequest } from '../../models/job-generation.models';

describe('JobGeneratorComponent', () => {
  let component: JobGeneratorComponent;
  let fixture: ComponentFixture<JobGeneratorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobGeneratorComponent, ReactiveFormsModule, CommonModule],

    })
      .compileComponents();

    fixture = TestBed.createComponent(JobGeneratorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('emits generateOffer and resets form when form is valid', () => {
    const payload: JobGenerationRequest = {
      ...MOCK_JOB_GENERATION_REQUEST
    };

    // populate form with valid values
    component.jobForm.setValue(payload);

    const emitSpy = spyOn(component.generateOffer, 'emit');

    // pre-condition: advanced may be true to ensure it is closed on submit
    component.isAdvancedOpen = true;

    component.generateJobOffer();

    expect(emitSpy).toHaveBeenCalledWith(payload);
    expect(component.isAdvancedOpen).toBeFalse();

    // after reset controls should not contain the previously submitted values
    // we check that the briefDescription is cleared/touched state reset
    const briefControl = component.jobForm.get('briefDescription');
    expect(briefControl?.value === '' || briefControl?.value === null).toBeTrue();
  });

  it('does not emit and marks controls touched when form is invalid', () => {
    // leave briefDescription invalid (required & minLength)
    component.jobForm.setValue({
      briefDescription: 'short',
      department: '',
      location: '',
      employmentType: '',
      salaryRange: '',
      tone: ''
    });

    const emitSpy = spyOn(component.generateOffer, 'emit');

    component.generateJobOffer();

    expect(emitSpy).not.toHaveBeenCalled();

    // all controls should be marked as touched
    Object.keys(component.jobForm.controls).forEach(key => {
      const ctrl = component.jobForm.get(key);
      expect(ctrl?.touched).toBeTrue();
    });
  });

  it('toggleAdvanced flips isAdvancedOpen', () => {
    const before = component.isAdvancedOpen;
    component.toggleAdvanced();
    expect(component.isAdvancedOpen).toBe(!before);
    component.toggleAdvanced();
    expect(component.isAdvancedOpen).toBe(before);
  });
});
