import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { CommonModule } from '@angular/common';
import { JobGenerationResponse } from '../../models/job-generation-response.models';
import { JobGenerationRequest } from '../../models/job-generation.models';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-job-preview',
  imports: [CommonModule, LoadingSpinnerComponent, ReactiveFormsModule],
  templateUrl: './job-preview.component.html',
  styleUrl: './job-preview.component.scss'
})
export class JobPreviewComponent {
  @Input() jobOffer: JobGenerationResponse | null = null;
  @Input() isLoading: boolean = false;
  @Input() originalBrief: JobGenerationRequest | null = null;

  @Output() regenerateOffer = new EventEmitter<void>();
  @Output() approveOffer = new EventEmitter<JobGenerationResponse>();

  editMode = false;
  editedJobForm: FormGroup | null = null;

  constructor(private fb: FormBuilder) { }

  onRegenerate(): void {
    this.regenerateOffer.emit();
  }

  onApprove(): void {
    if (!this.jobOffer || !this.editedJobForm) return;
    const val = this.editedJobForm.value;
    const updated: JobGenerationResponse = JSON.parse(JSON.stringify(this.jobOffer));

    updated.jobOffer.inferredTitle = val.inferredTitle;
    updated.jobOffer.level = val.level;
    updated.jobOffer.location = val.location;
    updated.jobOffer.salaryRange = val.salaryRange;
    updated.jobOffer.summary = val.summary;

    const toArray = (s: string) =>
      (s || '')
        .split('\n')
        .map((x: string) => x.trim())
        .filter((x: string) => x.length > 0);

    updated.jobOffer.responsibilities = toArray(val.responsibilities);
    updated.jobOffer.requiredQualifications = toArray(val.requiredQualifications);
    updated.jobOffer.preferredQualifications = toArray(val.preferredQualifications);
    updated.jobOffer.softSkills = toArray(val.softSkills);
    updated.jobOffer.benefits = toArray(val.benefits);

    updated.jobOffer.employmentType = val.employmentType;
    updated.jobOffer.tone = val.tone;

    this.jobOffer = updated;
    this.approveOffer.emit(updated);
    this.editMode = false;
    this.editedJobForm = null;
  }

  onEdit(): void {
    if (!this.jobOffer) return;
    const j = this.jobOffer.jobOffer;
    this.editedJobForm = this.fb.group({
      inferredTitle: [j.inferredTitle || '', Validators.required],
      level: [j.level || ''],
      location: [j.location || ''],
      salaryRange: [j.salaryRange || ''],
      summary: [j.summary || ''],
      responsibilities: [(j.responsibilities || []).join('\n')],
      requiredQualifications: [(j.requiredQualifications || []).join('\n')],
      preferredQualifications: [(j.preferredQualifications || []).join('\n')],
      softSkills: [(j.softSkills || []).join('\n')],
      benefits: [(j.benefits || []).join('\n')],
      employmentType: [j.employmentType || ''],
      tone: [j.tone || '']
    });
    this.editMode = true;
  }

  onCancelEdit(): void {
    this.editedJobForm = null;
    this.editMode = false;
  }
}
