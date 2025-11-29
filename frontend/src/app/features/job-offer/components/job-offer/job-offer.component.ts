import { Component, inject, OnInit } from '@angular/core';
import { JobGeneratorComponent } from '../job-generator/job-generator.component';
import { JobPreviewComponent } from '../job-preview/job-preview.component';
import { JobService } from '../../services/job.service';
import { JobGenerationRequest } from '../../models/job-generation.models';
import { JobGenerationResponse } from '../../models/job-generation-response.models';
import { Router } from '@angular/router';

@Component({
  selector: 'app-job-offer',
  imports: [JobGeneratorComponent, JobPreviewComponent],
  templateUrl: './job-offer.component.html',
  styleUrls: ['./job-offer.component.scss']
})
export class JobOfferComponent {
  generatedJobOffer: JobGenerationResponse | null = null;
  isLoading: boolean = false;
  originalBrief: JobGenerationRequest | null = null;
  error: string | null = null;
  private jobService = inject(JobService);

  private router = inject(Router);



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

  onApproveOffer(offer: JobGenerationResponse): void {
    if (!this.originalBrief) return;

    const approveRequest = {
      originalRequest: this.originalBrief,
      finalJobOffer: offer.jobOffer
    };

    this.jobService.approveJobOffer(approveRequest).subscribe({
      next: () => {
        this.generatedJobOffer = null;
        this.originalBrief = null;
        alert('Job offer approved successfully!');
      },
      error: (error) => {
        console.error('Error approving job offer:', error);
      }
    });
  }

  viewApproved(): void {
    // Logic to view approved job offers navigate to list page
    this.router.navigate(['/jobs/job-listing']);


  }
}
