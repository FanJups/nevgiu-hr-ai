import { Component } from '@angular/core';
import { JobGeneratorComponent } from '../job-generator/job-generator.component';
import { JobPreviewComponent } from '../job-preview/job-preview.component';

@Component({
  selector: 'app-job-offer',
  imports: [JobGeneratorComponent, JobPreviewComponent],
  templateUrl: './job-offer.component.html',
  styleUrl: './job-offer.component.scss'
})
export class JobOfferComponent {

}
