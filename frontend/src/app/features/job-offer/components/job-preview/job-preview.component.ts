import { Component, Input } from '@angular/core';
import { GeneratedJobOffer } from '../../models/generated-job-offer.models';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-job-preview',
  imports: [LoadingSpinnerComponent],
  templateUrl: './job-preview.component.html',
  styleUrl: './job-preview.component.scss'
})
export class JobPreviewComponent {
  @Input() jobOffer: GeneratedJobOffer | null = null;
  @Input() isLoading: boolean = false;
  @Input() originalBrief: string = '';
}
