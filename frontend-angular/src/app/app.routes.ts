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
    path: 'podcasts',
    pathMatch: 'full',
    loadComponent: () => import('./features/podcasts/podcasts.component'),
    title: 'Podcasts — Podcast Server',
  },
  {
    // Item detail must come before the podcast-detail route so that
    // /podcasts/:idPodcast/items/:id matches here instead of falling through.
    path: 'podcasts/:idPodcast/items/:id',
    loadComponent: () => import('./features/item-detail/item-detail.component'),
  },
  {
    path: 'podcasts/:idPodcast',
    loadComponent: () => import('./features/podcast-detail/podcast-detail.component'),
  },
  {
    path: 'playlists',
    pathMatch: 'full',
    loadComponent: () => import('./features/playlists/playlists.component'),
    title: 'Playlists — Podcast Server',
  },
  {
    // Item detail under a playlist — must come before the playlist-detail
    // route. Same component as the podcast route; the URL conveys the
    // "viewed from playlist" context (the podcastId is derived from the
    // playlist payload before the item is fetched).
    path: 'playlists/:idPlaylist/items/:id',
    loadComponent: () => import('./features/item-detail/item-detail.component'),
  },
  {
    path: 'playlists/:idPlaylist',
    loadComponent: () =>
      import('./features/playlist-detail/playlist-detail.component'),
  },
  {
    path: 'settings',
    pathMatch: 'full',
    loadComponent: () => import('./features/settings/settings.component'),
    title: 'Settings — Podcast Server',
  },
];
