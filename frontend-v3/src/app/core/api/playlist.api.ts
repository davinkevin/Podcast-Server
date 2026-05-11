import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource, HttpResourceRef } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import {
  PlaylistsContainerHAL,
  PlaylistWithItemsHAL,
} from '../models/playlist.model';
import { queryKeys } from './query-keys';

@Injectable({ providedIn: 'root' })
export class PlaylistApi {
  private readonly http = inject(HttpClient);

  list() {
    return injectQuery(() => ({
      queryKey: queryKeys.playlists.list(),
      queryFn: () =>
        lastValueFrom(
          this.http.get<PlaylistsContainerHAL>('/api/v1/playlists'),
        ),
    }));
  }

  getById(
    id: Signal<string | undefined>,
  ): HttpResourceRef<PlaylistWithItemsHAL | undefined> {
    return httpResource<PlaylistWithItemsHAL>(() => {
      const v = id();
      return v ? { url: `/api/v1/playlists/${v}` } : undefined;
    });
  }

  create(name: string, coverUrl?: string) {
    // Always omit `coverUrl` when blank — backend would otherwise try to
    // dereference an empty URI and 500. Only attach when it looks like a real
    // http(s) URL; the backend falls back to its placeholder cover otherwise.
    const url = coverUrl?.trim();
    const body: Record<string, string> =
      url && /^https?:\/\//.test(url) ? { name, coverUrl: url } : { name };
    return this.http.post<PlaylistWithItemsHAL>('/api/v1/playlists', body);
  }

  delete(id: string) {
    return this.http.delete(`/api/v1/playlists/${id}`, { responseType: 'text' });
  }

  addItem(playlistId: string, itemId: string) {
    return this.http.post<PlaylistWithItemsHAL>(
      `/api/v1/playlists/${playlistId}/items/${itemId}`,
      null,
    );
  }

  removeItem(playlistId: string, itemId: string) {
    return this.http.delete<PlaylistWithItemsHAL>(
      `/api/v1/playlists/${playlistId}/items/${itemId}`,
    );
  }
}
