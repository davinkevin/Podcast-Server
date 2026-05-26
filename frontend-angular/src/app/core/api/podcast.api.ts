import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import {
  PodcastCreationHAL,
  PodcastHAL,
  PodcastsContainerHAL,
  PodcastUpdateHAL,
} from '../models/podcast.model';
import { PageHAL } from '../models/page.model';
import { ItemHAL } from '../models/item.model';
import { queryKeys } from './query-keys';

export interface PodcastItemsInput {
  readonly podcastId: string;
  readonly q?: string;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
}

@Injectable({ providedIn: 'root' })
export class PodcastApi {
  private readonly http = inject(HttpClient);
  private readonly queryClient = inject(QueryClient);

  list() {
    return injectQuery(() => ({
      queryKey: queryKeys.podcasts.list(),
      queryFn: () =>
        lastValueFrom(this.http.get<PodcastsContainerHAL>('/api/v1/podcasts')),
    }));
  }

  // Imperative warm-up of the list cache from outside a component (e.g. the
  // command palette service). Returns immediately if the cache is fresh,
  // otherwise kicks off the fetch and resolves when it lands. No subscription
  // is created — readers using `list()` pick the cached data up reactively.
  prefetchList() {
    return this.queryClient.prefetchQuery({
      queryKey: queryKeys.podcasts.list(),
      queryFn: () =>
        lastValueFrom(this.http.get<PodcastsContainerHAL>('/api/v1/podcasts')),
    });
  }

  getById(id: Signal<string | undefined>) {
    return injectQuery(() => {
      const v = id();
      return {
        queryKey: v ? queryKeys.podcasts.detail(v) : ['podcasts', 'detail', 'noop'],
        queryFn: () =>
          lastValueFrom(this.http.get<PodcastHAL>(`/api/v1/podcasts/${v}`)),
        enabled: !!v,
      };
    });
  }

  items(input: Signal<PodcastItemsInput | undefined>) {
    return injectQuery(() => {
      const f = input();
      return {
        queryKey: f
          ? queryKeys.podcasts.items(f)
          : ['podcasts', 'items', 'noop'],
        queryFn: () => {
          const params = new HttpParams()
            .set('q', f!.q ?? '')
            .set('page', f!.page ?? 0)
            .set('size', f!.size ?? 24)
            .set('sort', f!.sort ?? 'pubDate,DESC');
          return lastValueFrom(
            this.http.get<PageHAL<ItemHAL>>(
              `/api/v1/podcasts/${f!.podcastId}/items`,
              { params },
            ),
          );
        },
        enabled: !!f,
      };
    });
  }

  createMutation() {
    return injectMutation(() => ({
      mutationFn: (body: PodcastCreationHAL) =>
        lastValueFrom(this.http.post<PodcastHAL>('/api/v1/podcasts', body)),
      onSuccess: () => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.podcasts.list(),
        });
      },
    }));
  }

  updateMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { id: string; body: PodcastUpdateHAL }) =>
        lastValueFrom(
          this.http.put<PodcastHAL>(`/api/v1/podcasts/${args.id}`, args.body),
        ),
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.podcasts.detail(vars.id),
        });
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.podcasts.list(),
        });
      },
    }));
  }

  deleteMutation() {
    return injectMutation(() => ({
      mutationFn: (id: string) =>
        lastValueFrom(
          this.http.delete(`/api/v1/podcasts/${id}`, { responseType: 'text' }),
        ),
      onSuccess: () => {
        this.queryClient.invalidateQueries({ queryKey: queryKeys.podcasts.all });
      },
    }));
  }

  uploadMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { podcastId: string; file: File }) => {
        const form = new FormData();
        form.append('file', args.file, args.file.name);
        return lastValueFrom(
          this.http.post<ItemHAL>(
            `/api/v1/podcasts/${args.podcastId}/items/upload`,
            form,
          ),
        );
      },
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({
          queryKey: ['podcasts', vars.podcastId, 'items'],
        });
      },
    }));
  }

  // Fire-and-forget HTTP, SSE drives the UI update. Kept as Observable since
  // there's no related query to invalidate eagerly. `download: true` makes
  // the backend chain a `launchDownload()` once the refresh has produced
  // new items — equivalent to clicking "Update now" then immediately
  // "Download" on each new episode.
  triggerUpdate(id: string, options: { readonly download?: boolean } = {}) {
    const params: Record<string, string> = {};
    if (options.download) params['download'] = 'true';
    return this.http.get(`/api/v1/podcasts/${id}/update`, {
      params,
      responseType: 'text',
    });
  }

  updateAll(options: { readonly download?: boolean; readonly force?: boolean } = {}) {
    const params: Record<string, string> = {};
    if (options.download) params['download'] = 'true';
    if (options.force) params['force'] = 'true';
    return this.http.get('/api/v1/podcasts/update', {
      params,
      responseType: 'text',
    });
  }
}
