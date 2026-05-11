import { computed, inject, Injectable, Signal } from '@angular/core';
import {
  HttpClient,
  HttpParams,
  httpResource,
  HttpResourceRef,
} from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { PageHAL } from '../models/page.model';
import { ItemHAL, ItemStatus } from '../models/item.model';
import { PlaylistsContainerHAL } from '../models/playlist.model';
import { queryKeys } from './query-keys';

export interface ItemSearchInput {
  readonly q?: string;
  readonly tags?: readonly string[];
  readonly status?: readonly ItemStatus[];
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
}

export interface ItemRef {
  readonly podcastId: string;
  readonly id: string;
}

@Injectable({ providedIn: 'root' })
export class ItemApi {
  private readonly http = inject(HttpClient);

  search(input: Signal<ItemSearchInput>) {
    return injectQuery(() => {
      const f = input();
      return {
        queryKey: queryKeys.items.search(f),
        queryFn: () => {
          let params = new HttpParams()
            .set('q', f.q ?? '')
            .set('page', f.page ?? 0)
            .set('size', f.size ?? 12)
            .set('sort', f.sort ?? 'pubDate,DESC');
          if (f.tags && f.tags.length > 0) {
            params = params.set('tags', f.tags.join(','));
          }
          if (f.status && f.status.length > 0) {
            params = params.set('status', f.status.join(','));
          }
          return lastValueFrom(
            this.http.get<PageHAL<ItemHAL>>('/api/v1/items/search', { params }),
          );
        },
      };
    });
  }

  getById(input: Signal<ItemRef | undefined>): HttpResourceRef<ItemHAL | undefined> {
    return httpResource<ItemHAL>(() => {
      const ref = input();
      if (!ref) return undefined;
      return { url: `/api/v1/podcasts/${ref.podcastId}/items/${ref.id}` };
    });
  }

  triggerDownload(podcastId: string, itemId: string) {
    return this.http.post(
      `/api/v1/podcasts/${podcastId}/items/${itemId}/download`,
      null,
      { responseType: 'text' },
    );
  }

  reset(podcastId: string, itemId: string) {
    return this.http.post<ItemHAL>(
      `/api/v1/podcasts/${podcastId}/items/${itemId}/reset`,
      null,
    );
  }

  delete(podcastId: string, itemId: string) {
    return this.http.delete(`/api/v1/podcasts/${podcastId}/items/${itemId}`, {
      responseType: 'text',
    });
  }

  /** Deletes downloaded items older than `days` (defaults to backend's 30). */
  cleanup(days: number) {
    return this.http.delete(`/api/v1/items?days=${days}`, {
      responseType: 'text',
    });
  }

  /** Lists playlists currently containing the given item. */
  playlistsContaining(
    ref: Signal<{ readonly podcastId: string; readonly itemId: string } | undefined>,
  ): HttpResourceRef<PlaylistsContainerHAL | undefined> {
    return httpResource<PlaylistsContainerHAL>(() => {
      const r = ref();
      return r
        ? { url: `/api/v1/podcasts/${r.podcastId}/items/${r.itemId}/playlists` }
        : undefined;
    });
  }
}
