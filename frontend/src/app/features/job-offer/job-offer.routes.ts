import { Routes } from "@angular/router";

export const JOB_ROUTES: Routes = [
    {
        path: "", // Default route
        loadComponent: () =>
            import("./components/job-offer/job-offer.component").then(
                (m) => m.JobOfferComponent
            ),
    },

];