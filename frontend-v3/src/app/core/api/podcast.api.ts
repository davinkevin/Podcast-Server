import { computed, inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource, HttpResourceRef } from '@angular/common/http';

import {
  PodcastCreationHAL,
  PodcastHAL,
  PodcastsContainerHAL,
  PodcastUpdateHAL,
} from '../models/podcast.model';
import { PageHAL } from '../models/page.model';
import { ItemHAL } from '../models/item.model';

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

  list(): HttpResourceRef<PodcastsContainerHAL | undefined> {
    return httpResource<PodcastsContainerHAL>(() => ({ url: '/api/v1/podcasts' }));
  }

  getById(id: Signal<string | undefined>): HttpResourceRef<PodcastHAL | undefined> {
    return httpResource<PodcastHAL>(() => {
      const v = id();
      return v ? { url: `/api/v1/podcasts/${v}` } : undefined;
    });
  }

  items(input: Signal<PodcastItemsInput | undefined>): HttpResourceRef<PageHAL<ItemHAL> | undefined> {
    return httpResource<PageHAL<ItemHAL>>(() => {
      const f = input();
      if (!f) return undefined;
      return {
        url: `/api/v1/podcasts/${f.podcastId}/items`,
        params: {
          q: f.q ?? '',
          page: f.page ?? 0,
          size: f.size ?? 24,
          sort: f.sort ?? 'pubDate,DESC',
        },
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

  delete(id: string) {
    return this.http.delete(`/api/v1/podcasts/${id}`, { responseType: 'text' });
  }

  upload(podcastId: string, file: File) {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ItemHAL>(`/api/v1/podcasts/${podcastId}/items/upload`, form);
  }
}
