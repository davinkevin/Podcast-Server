import { ApplicationConfig, inject, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  Params,
  provideRouter,
  Router,
  RouteReuseStrategy,
  withComponentInputBinding,
  withViewTransitions,
} from '@angular/router';
import { provideTanStackQuery, QueryClient } from '@tanstack/angular-query-experimental';

import { routes } from './app.routes';
import { ListRouteReuseStrategy } from './core/navigation/list-route-reuse.strategy';

function sameParams(a: Params, b: Params): boolean {
  const keys = Object.keys(a);
  return keys.length === Object.keys(b).length && keys.every((k) => a[k] === b[k]);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
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
        onViewTransitionCreated: ({ transition, from, to }) => {
          const trigger = inject(Router).currentNavigation()?.trigger;
          if (trigger === 'popstate') {
            transition.skipTransition();
            return;
          }

          // A query-string-only change — a filter, a page, a sort — is not a
          // move between pages, so it gets no transition. Two reasons. It is
          // gratuitous motion: the grid cross-fades every time a filter is
          // nudged. And it is actively wrong on top of an anchored overlay,
          // because named elements are lifted into the view transition's own
          // pseudo-element tree, which paints above the top layer — so the
          // covers flew over the open filters panel before dropping back
          // behind it. Same route and same path params means same page.
          if (from.routeConfig === to.routeConfig && sameParams(from.params, to.params)) {
            transition.skipTransition();
          }
        },
      }),
    ),
    provideHttpClient(withFetch()),
    // Detach (instead of destroy) the spotlight/list routes and the
    // podcast/playlist detail routes when navigating away. On return the
    // original DOM — including already-decoded covers — is re-attached,
    // avoiding the iOS swipe-back flicker caused by the bfcache snapshot
    // being swapped for a freshly rendered DOM tree.
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
