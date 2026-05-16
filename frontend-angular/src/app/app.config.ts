import {
  ApplicationConfig,
  inject,
  provideExperimentalZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  provideRouter,
  Router,
  RouteReuseStrategy,
  withComponentInputBinding,
  withViewTransitions,
} from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import {
  provideTanStackQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';

import { routes } from './app.routes';
import { ListRouteReuseStrategy } from './core/navigation/list-route-reuse.strategy';

export const appConfig: ApplicationConfig = {
  providers: [
    provideExperimentalZonelessChangeDetection(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      // Native View Transitions API (Chrome 111+, Safari 18+, Firefox 129+).
      // Default behavior is a cross-fade between the old and new page.
      //
      // On `popstate` (browser back/forward + iOS edge-swipe-to-go-back),
      // skip our transition. iOS Safari plays its own native swipe animation
      // from bfcache that we can't suppress — running our morph on top of
      // that produces a visible double-animation. Letting iOS own that
      // navigation keeps the UX clean; forward (imperative) navigations
      // keep their morph.
      withViewTransitions({
        onViewTransitionCreated: ({ transition }) => {
          const trigger = inject(Router).getCurrentNavigation()?.trigger;
          if (trigger === 'popstate') transition.skipTransition();
        },
      }),
    ),
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    // Detach (instead of destroy) the list routes when navigating away to a
    // detail page. On return the original DOM — including already-decoded
    // covers — is re-attached, avoiding the iOS swipe-back cover flicker
    // caused by the bfcache snapshot being swapped for a freshly rendered
    // DOM tree.
    { provide: RouteReuseStrategy, useClass: ListRouteReuseStrategy },
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
