import { ComponentRef, Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  BaseRouteReuseStrategy,
  DetachedRouteHandle,
} from '@angular/router';

/**
 * Path segments whose components keep their DOM/state across navigations.
 * The pages keep their rendered <img> grid alive so iOS swipe-back doesn't
 * trigger a full remount + flicker on every cover.
 *
 * `''` is the spotlight/landing page — beware: the empty path is falsy, so
 * every key check below compares against `null`, never truthiness.
 */
const RETAINED_PATHS = new Set(['', 'library', 'podcasts', 'playlists']);

/**
 * Parameterized detail pages, retained so browser-back from item-detail
 * re-attaches the exact same DOM instead of remounting (cover + items
 * flicker on iOS). Keyed by the resolved URL — /podcasts/A must never
 * reuse /podcasts/B's DOM — and bounded to the most recent instance per
 * route config: visiting another podcast destroys the previous handle.
 */
const RETAINED_DETAIL_PATHS = new Set([
  'podcasts/:idPodcast',
  'playlists/:idPlaylist',
]);

@Injectable({ providedIn: 'root' })
export class ListRouteReuseStrategy extends BaseRouteReuseStrategy {
  private readonly handles = new Map<string, DetachedRouteHandle>();

  override shouldDetach(route: ActivatedRouteSnapshot): boolean {
    return this.keyOf(route) !== null;
  }

  override store(
    route: ActivatedRouteSnapshot,
    handle: DetachedRouteHandle | null,
  ): void {
    const key = this.keyOf(route);
    if (key === null) return;
    if (handle) {
      this.evictSiblingsOf(key);
      this.handles.set(key, handle);
    } else {
      this.handles.delete(key);
    }
  }

  override shouldAttach(route: ActivatedRouteSnapshot): boolean {
    const key = this.keyOf(route);
    return key !== null && this.handles.has(key);
  }

  override retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    const key = this.keyOf(route);
    return key === null ? null : (this.handles.get(key) ?? null);
  }

  private keyOf(route: ActivatedRouteSnapshot): string | null {
    const config = route.routeConfig?.path;
    if (config === undefined) return null;
    if (RETAINED_PATHS.has(config)) return config;
    if (RETAINED_DETAIL_PATHS.has(config)) {
      return `${config}|${route.url.map((s) => s.path).join('/')}`;
    }
    return null;
  }

  /**
   * Drop any handle stored for the same detail route config but a
   * different URL (e.g. another podcast). A dropped handle must be
   * destroyed explicitly, otherwise the detached component leaks.
   */
  private evictSiblingsOf(key: string): void {
    const separator = key.indexOf('|');
    if (separator === -1) return;
    const prefix = key.slice(0, separator + 1);
    for (const [k, handle] of this.handles) {
      if (k === key || !k.startsWith(prefix)) continue;
      // DetachedRouteHandle is opaque, but the router stores the
      // ComponentRef under `componentRef` — destroying it runs the
      // component's DestroyRef/effect cleanups.
      (handle as { componentRef?: ComponentRef<unknown> }).componentRef?.destroy();
      this.handles.delete(k);
    }
  }
}
