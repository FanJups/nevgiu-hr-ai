import { Component, inject } from '@angular/core';
import { JobGeneratorComponent } from '../job-generator/job-generator.component';
import { JobPreviewComponent } from '../job-preview/job-preview.component';
import { JobService } from '../../services/job.service';
import { JobGenerationRequest } from '../../models/job-generation.models';
import { JobGenerationResponse } from '../../models/job-generation-response.models';

@Component({
  selector: 'app-job-offer',
  imports: [JobGeneratorComponent, JobPreviewComponent],
  templateUrl: './job-offer.component.html',
  styleUrl: './job-offer.component.scss'
})
export class JobOfferComponent {
  generatedJobOffer: JobGenerationResponse | null = null;
  isLoading: boolean = false;
  originalBrief: JobGenerationRequest | null = null;
  error: string | null = null;
  private jobService = inject(JobService);

  onGenerateOffer(request: JobGenerationRequest): void {
    this.isLoading = true;
    this.error = null;
    this.originalBrief = request;

    this.jobService.generateJobOffer(request).subscribe({
      next: (response) => {
        this.generatedJobOffer = response;
        console.log('Generated Job Offer:', this.generatedJobOffer);
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to generate job offer. Please try again.';
        this.isLoading = false;
        console.error('Error generating job offer:', error);
      }
    });
  }

  onRegenerateOffer(): void {
    if (this.originalBrief) {
      const request: JobGenerationRequest = {
        ...this.originalBrief
      };
      this.onGenerateOffer(request);
    }
  }
}
