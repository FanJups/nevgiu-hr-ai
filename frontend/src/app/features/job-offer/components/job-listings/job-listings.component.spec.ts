import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobListingsComponent } from './job-listings.component';
import { By } from '@angular/platform-browser';
import { JobService } from '../../services/job.service';

const MOCK_APPROVED_JOBS = [
  { inferredTitle: 'Backend Engineer', level: 'Senior', summary: 'Build services' },
  { inferredTitle: 'Frontend Engineer', level: 'Mid', summary: 'Build UIs' }
];

describe('JobListingsComponent', () => {
  let component: JobListingsComponent;
  let fixture: ComponentFixture<JobListingsComponent>;
  let jobServiceSpy: jasmine.SpyObj<JobService>;

  beforeEach(async () => {
    jobServiceSpy = jasmine.createSpyObj('JobService', ['getJobs']);
    await TestBed.configureTestingModule({
      imports: [JobListingsComponent],
      providers: [
        { provide: JobService, useValue: jobServiceSpy }
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(JobListingsComponent);
    component = fixture.componentInstance;

  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders header and descriptive text', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('Approved Job Offers');
    expect(el.querySelector('.header p')?.textContent).toContain('View and manage all approved job offers');
  });


  it('renders no table rows when approvedJobs is empty', () => {
    component.approvedJobs = [];


    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows.length).toBe(0);
  });
});
