import { Routes } from '@angular/router';

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
    },
    {
        path: "candidates",
        loadChildren: () =>
            import("./features/cv-ingestion/cv-ingestion.routes").then((m) => m.CV_INGESTION_ROUTES),
    },
    { path: "**", redirectTo: "jobs/job-offer" },
];
