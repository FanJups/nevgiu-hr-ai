import { Component, inject, OnInit } from '@angular/core';
import { JobGenerationResponse } from '../../models/job-generation-response.models';
import { JobService } from '../../services/job.service';
import { CommonModule } from '@angular/common';
import { GeneratedJobOffer } from '../../models/generated-job-offer.models';

@Component({
  selector: 'app-job-listings',
  imports: [CommonModule],
  templateUrl: './job-listings.component.html',
  styleUrl: './job-listings.component.scss'
})
export class JobListingsComponent implements OnInit {
  approvedJobs: GeneratedJobOffer[] = [];

  private jobService = inject(JobService);

  ngOnInit(): void {
    this.fetchApprovedJobs();
  }

  private fetchApprovedJobs(): void {
    this.jobService.getJobs().subscribe({
      next: (jobs) => {
        console.log('Fetched approved jobs:', jobs);
        this.approvedJobs = jobs;
      },
      error: (error) => {
        console.error('Error fetching approved jobs:', error);
      }
    });
  }
}
