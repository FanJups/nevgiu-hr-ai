import { CV_INGESTION_ROUTES } from './cv-ingestion.routes';

describe('CV ingestion routes', () => {
  it('exposes the import workspace and redirects the feature root', () => {
    expect(CV_INGESTION_ROUTES.find((route) => route.path === 'import')?.loadComponent).toBeDefined();
    expect(CV_INGESTION_ROUTES.find((route) => route.path === '')?.redirectTo).toBe('import');
  });
});
