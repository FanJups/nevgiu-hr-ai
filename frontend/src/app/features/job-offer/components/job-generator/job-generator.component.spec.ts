import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobGeneratorComponent } from './job-generator.component';

describe('JobGeneratorComponent', () => {
  let component: JobGeneratorComponent;
  let fixture: ComponentFixture<JobGeneratorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobGeneratorComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JobGeneratorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
