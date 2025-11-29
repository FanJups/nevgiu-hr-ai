import { Routes } from "@angular/router";

export const JOB_ROUTES: Routes = [
    {
        path: "job-offer", // Default route
        loadComponent: () =>
            import("./components/job-offer/job-offer.component").then(
                (m) => m.JobOfferComponent
            ),
    },
    {
        path: "job-listing", // Default route
        loadComponent: () =>
            import("./components/job-listings/job-listings.component").then(
                (m) => m.JobListingsComponent
            ),
    },
];