import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, Routes } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';

import { AppComponent } from './app/app.component';

// Lazy loaded routes - laadib komponente alles vajaduse korral
const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./app/pages/dashboard/dashboard.component')
      .then(m => m.DashboardComponent),
    title: 'Dashboard - Eesti IT Töökohad'
  },
  {
    path: 'jobs',
    loadComponent: () => import('./app/pages/jobs/jobs.component')
      .then(m => m.JobsComponent),
    title: 'Tööpakkumised - Eesti IT Töökohad'
  },
  {
    path: 'jobs/:id',
    loadComponent: () => import('./app/pages/job-detail/job-detail.component')
      .then(m => m.JobDetailComponent),
    title: 'Tööpakkumine - Eesti IT Töökohad'
  },
  {
    path: 'trends',
    loadComponent: () => import('./app/pages/trends/trends.component')
      .then(m => m.TrendsComponent),
    title: 'Trendid - Eesti IT Töökohad'
  },
  {
    path: '**',
    redirectTo: ''
  }
];

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations()
  ]
}).catch(err => console.error(err));
