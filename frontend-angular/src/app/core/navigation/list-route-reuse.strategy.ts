import { Injectable } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  BaseRouteReuseStrategy,
  DetachedRouteHandle,
} from '@angular/router';

/**
 * Path segments whose components keep their DOM/state across navigations.
 * The lists keep their rendered <img> grid alive so iOS swipe-back doesn't
 * trigger a full remount + flicker on every cover.
 */
const RETAINED_PATHS = new Set(['library', 'podcasts', 'playlists']);

@Injectable({ providedIn: 'root' })
export class ListRouteReuseStrategy extends BaseRouteReuseStrategy {
  private readonly handles = new Map<string, DetachedRouteHandle>();

  override shouldDetach(route: ActivatedRouteSnapshot): boolean {
    return RETAINED_PATHS.has(route.routeConfig?.path ?? '');
  }

  override store(
    route: ActivatedRouteSnapshot,
    handle: DetachedRouteHandle | null,
  ): void {
    const key = route.routeConfig?.path;
    if (!key) return;
    if (handle) this.handles.set(key, handle);
    else this.handles.delete(key);
  }

  override shouldAttach(route: ActivatedRouteSnapshot): boolean {
    const key = route.routeConfig?.path;
    return key !== undefined && this.handles.has(key);
  }

  override retrieve(route: ActivatedRouteSnapshot): DetachedRouteHandle | null {
    const key = route.routeConfig?.path;
    return key ? this.handles.get(key) ?? null : null;
  }
}
