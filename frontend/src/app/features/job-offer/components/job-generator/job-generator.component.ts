import { LoadingSpinnerComponent } from './../../../../shared/components/loading-spinner/loading-spinner.component';
import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { JobService } from '../../services/job.service';
import { JobGenerationRequest } from '../../models/job-generation.models';

@Component({
  selector: 'app-job-generator',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './job-generator.component.html',
  styleUrl: './job-generator.component.scss'
})
export class JobGeneratorComponent {
  @Output() generateOffer = new EventEmitter<JobGenerationRequest>();
  @Input() loading = false;

  jobForm: FormGroup;
  isAdvancedOpen = false;

  employmentTypes = ['Full-time', 'Part-time', 'Contract', 'Internship'];
  tones = [
    { value: 'formal', label: 'Formal' },
    { value: 'friendly', label: 'Friendly' },
    { value: 'inclusive', label: 'Inclusive' }
  ];

  constructor(private fb: FormBuilder) {
    this.jobForm = this.createForm();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      briefDescription: ['', [Validators.required, Validators.minLength(10)]],
      department: [''],
      location: [''],
      employmentType: [''],
      salaryRange: [''],
      tone: ['']
    });
  }

  generateJobOffer(): void {
    if (this.jobForm.valid) {
      this.generateOffer.emit(this.jobForm.value);
      this.jobForm.reset();
      this.isAdvancedOpen = false;
    } else {
      this.markFormGroupTouched();
    }
  }

  toggleAdvanced(): void {
    this.isAdvancedOpen = !this.isAdvancedOpen;
  }

  private markFormGroupTouched(): void {
    Object.keys(this.jobForm.controls).forEach(key => {
      this.jobForm.get(key)?.markAsTouched();
    });
  }

  get briefDescription() {
    return this.jobForm.get('briefDescription');
  }
}
