import { computed, inject, Injectable, Signal } from '@angular/core';
import {
  HttpClient,
  HttpParams,
  httpResource,
  HttpResourceRef,
} from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
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

  list() {
    return injectQuery(() => ({
      queryKey: queryKeys.podcasts.list(),
      queryFn: () =>
        lastValueFrom(this.http.get<PodcastsContainerHAL>('/api/v1/podcasts')),
    }));
  }

  getById(id: Signal<string | undefined>): HttpResourceRef<PodcastHAL | undefined> {
    return httpResource<PodcastHAL>(() => {
      const v = id();
      return v ? { url: `/api/v1/podcasts/${v}` } : undefined;
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

  create(body: PodcastCreationHAL) {
    return this.http.post<PodcastHAL>('/api/v1/podcasts', body);
  }

  update(id: string, body: PodcastUpdateHAL) {
    return this.http.put<PodcastHAL>(`/api/v1/podcasts/${id}`, body);
  }

  triggerUpdate(id: string) {
    return this.http.get(`/api/v1/podcasts/${id}/update`, { responseType: 'text' });
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

  delete(id: string) {
    return this.http.delete(`/api/v1/podcasts/${id}`, { responseType: 'text' });
  }

  upload(podcastId: string, file: File) {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ItemHAL>(`/api/v1/podcasts/${podcastId}/items/upload`, form);
  }
}
