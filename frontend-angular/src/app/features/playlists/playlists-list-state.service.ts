import { Injectable, signal } from '@angular/core';

/**
 * In-memory state for the `/playlists` list page that survives across
 * navigation. The component instance is destroyed when the user leaves the
 * route; this `providedIn: 'root'` service stays alive for the session so
 * the search filter is still populated when they return.
 *
 * Not persisted across full page reloads — that's intentional: search is
 * a transient filter, not a user preference.
 */
@Injectable({ providedIn: 'root' })
export class PlaylistsListStateService {
  readonly search = signal('');
}
