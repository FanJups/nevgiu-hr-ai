import { Routes } from '@angular/router';

export const CV_INGESTION_ROUTES: Routes = [
  {
    path: 'import',
    loadComponent: () => import('./components/cv-workspace/cv-workspace.component')
      .then((m) => m.CvWorkspaceComponent),
  },
  { path: '', redirectTo: 'import', pathMatch: 'full' },
];
