import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-job-generator',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './job-generator.component.html',
  styleUrl: './job-generator.component.scss'
})
export class JobGeneratorComponent {
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
      briefDescription: ['', [Validators.required, Validators.minLength(20)]],
      department: [''],
      location: [''],
      employmentType: ['Full-time'],
      salaryRange: [''],
      tone: ['formal']
    });
  }

  toggleAdvanced(): void {
    this.isAdvancedOpen = !this.isAdvancedOpen;
  }

  get briefDescription() {
    return this.jobForm.get('briefDescription');
  }
}
