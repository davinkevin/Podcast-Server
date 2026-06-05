import { Injectable, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';

/**
 * Router URL after each completed navigation, as a signal.
 *
 * Components retained by ListRouteReuseStrategy are detached — not
 * destroyed — so they get no lifecycle hook on re-attach. Reading this
 * signal inside an effect makes the effect re-run on the change detection
 * cycle that re-inserts the view, which is the "I'm back on screen" hook
 * those components need.
 */
@Injectable({ providedIn: 'root' })
export class CurrentUrlService {
  private readonly router = inject(Router);
  private readonly currentUrl = signal(this.router.url);

  readonly url = this.currentUrl.asReadonly();

  constructor() {
    // Root-scoped service: lives as long as the app, no teardown needed.
    this.router.events.subscribe((e) => {
      if (e instanceof NavigationEnd) this.currentUrl.set(this.router.url);
    });
  }
}
