import { Injectable } from '@angular/core';

/**
 * In-memory cache for paginated/resource responses keyed by an arbitrary
 * string scope. Used by detail routes to display their last-known items
 * immediately on return navigation so view-transition morphs from the
 * detail page back to a card on the list can find their target.
 *
 * Not a long-lived cache — values survive in-app navigation only; a full
 * reload empties it.
 */
@Injectable({ providedIn: 'root' })
export class PageCache {
  private readonly store = new Map<string, unknown>();

  put<T>(key: string, value: T): void {
    this.store.set(key, value);
  }

  get<T>(key: string): T | undefined {
    return this.store.get(key) as T | undefined;
  }
}
