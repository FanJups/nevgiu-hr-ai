import { Routes } from '@angular/router';
import { JobService } from './features/job-offer/services/job.service';

export const routes: Routes = [
    {
        path: "", // Default route
        redirectTo: "jobs/job-offer",
        pathMatch: "full",
    },
    {
        path: "jobs",
        loadChildren: () =>
            import("./features/job-offer/job-offer.routes").then((m) => m.JOB_ROUTES),
        providers: [
            JobService // and their child components
        ],
    },
];
