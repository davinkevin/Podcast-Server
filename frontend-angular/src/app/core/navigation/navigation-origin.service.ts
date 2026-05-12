import { Injectable } from '@angular/core';

export type NavigationOrigin = 'library' | 'podcast' | 'playlist';

/**
 * Transient origin marker for navigations into item-detail. Used to scope
 * the View Transitions `view-transition-name` on the hero cover so it only
 * morphs with the list page that triggered the navigation.
 *
 * In-memory: set by the source list right before `router.navigate(...)`,
 * consumed once when item-detail mounts. SPA navigation stays in the same
 * JS context so memory is enough; a full reload simply yields `null` (no
 * morph), which is the correct behavior since the source DOM is gone.
 */
@Injectable({ providedIn: 'root' })
export class NavigationOriginService {
  private value: NavigationOrigin | null = null;

  set(value: NavigationOrigin): void {
    this.value = value;
  }

  consume(): NavigationOrigin | null {
    const v = this.value;
    this.value = null;
    return v;
  }
}
