import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'library',
  },
  {
    path: 'library',
    loadComponent: () => import('./features/library/library.component'),
    title: 'Library — Podcast Server',
  },
  {
    path: 'podcasts/:idPodcast/items/:id',
    loadComponent: () => import('./features/item-detail/item-detail.component'),
  },
];
