import {
  ApplicationConfig,
  provideExperimentalZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  provideRouter,
  withComponentInputBinding,
  withViewTransitions,
} from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  provideTanStackQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideExperimentalZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // Native View Transitions API (Chrome 111+, Safari 18+, Firefox 129+).
      // Default behavior is a cross-fade between the old and new page.
      withViewTransitions(),
    ),
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    // TanStack Query — server state caching, stale-while-revalidate,
    // invalidation. Replaces the ad-hoc PageCache + httpResource pattern.
    provideTanStackQuery(
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60_000,
            gcTime: 5 * 60_000,
          },
        },
      }),
    ),
  ],
};
