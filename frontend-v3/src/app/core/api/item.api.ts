import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';
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
  private readonly queryClient = inject(QueryClient);

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

  getById(input: Signal<ItemRef | undefined>) {
    return injectQuery(() => {
      const ref = input();
      return {
        queryKey: ref
          ? queryKeys.items.detail(ref.podcastId, ref.id)
          : ['items', 'detail', 'noop'],
        queryFn: () =>
          lastValueFrom(
            this.http.get<ItemHAL>(
              `/api/v1/podcasts/${ref!.podcastId}/items/${ref!.id}`,
            ),
          ),
        enabled: !!ref,
      };
    });
  }

  /** Lists playlists currently containing the given item. */
  playlistsContaining(
    ref: Signal<{ readonly podcastId: string; readonly itemId: string } | undefined>,
  ) {
    return injectQuery(() => {
      const r = ref();
      return {
        queryKey: r
          ? queryKeys.items.playlistsContaining(r.podcastId, r.itemId)
          : ['items', 'playlists-containing', 'noop'],
        queryFn: () =>
          lastValueFrom(
            this.http.get<PlaylistsContainerHAL>(
              `/api/v1/podcasts/${r!.podcastId}/items/${r!.itemId}/playlists`,
            ),
          ),
        enabled: !!r,
      };
    });
  }

  // SSE-driven; the queue/downloading signals update on their own. Kept as
  // Observable since there's no related query to invalidate eagerly.
  triggerDownload(podcastId: string, itemId: string) {
    return this.http.post(
      `/api/v1/podcasts/${podcastId}/items/${itemId}/download`,
      null,
      { responseType: 'text' },
    );
  }

  resetMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { podcastId: string; itemId: string }) =>
        lastValueFrom(
          this.http.post<ItemHAL>(
            `/api/v1/podcasts/${args.podcastId}/items/${args.itemId}/reset`,
            null,
          ),
        ),
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.items.detail(vars.podcastId, vars.itemId),
        });
        this.queryClient.invalidateQueries({
          queryKey: ['podcasts', vars.podcastId, 'items'],
        });
      },
    }));
  }

  deleteMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { podcastId: string; itemId: string }) =>
        lastValueFrom(
          this.http.delete(
            `/api/v1/podcasts/${args.podcastId}/items/${args.itemId}`,
            { responseType: 'text' },
          ),
        ),
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
        this.queryClient.invalidateQueries({
          queryKey: ['podcasts', vars.podcastId, 'items'],
        });
      },
    }));
  }

  cleanupMutation() {
    return injectMutation(() => ({
      mutationFn: (days: number) =>
        lastValueFrom(
          this.http.delete(`/api/v1/items?days=${days}`, { responseType: 'text' }),
        ),
      onSuccess: () => {
        this.queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
      },
    }));
  }
}
