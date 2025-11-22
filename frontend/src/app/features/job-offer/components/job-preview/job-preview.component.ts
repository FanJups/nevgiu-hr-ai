import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { CommonModule } from '@angular/common';
import { JobGenerationResponse } from '../../models/job-generation-response.models';
import { JobGenerationRequest } from '../../models/job-generation.models';

@Component({
  selector: 'app-job-preview',
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './job-preview.component.html',
  styleUrl: './job-preview.component.scss'
})
export class JobPreviewComponent {
  @Input() jobOffer: JobGenerationResponse | null = null;
  @Input() isLoading: boolean = false;
  @Input() originalBrief: JobGenerationRequest | null = null;

  @Output() regenerateOffer = new EventEmitter<void>();

  onRegenerate(): void {
    this.regenerateOffer.emit();
  }
}
