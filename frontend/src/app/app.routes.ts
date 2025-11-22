import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: "job-offer",
        loadChildren: () =>
            import("./features/job-offer/job-offer.routes").then((m) => m.JOB_ROUTES),
    },
];
