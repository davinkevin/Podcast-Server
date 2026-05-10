import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, httpResource, HttpResourceRef } from '@angular/common/http';

import { PageHAL } from '../models/page.model';
import { ItemHAL, ItemStatus } from '../models/item.model';

export interface ItemSearchInput {
  readonly q?: string;
  readonly tags?: readonly string[];
  readonly status?: readonly ItemStatus[];
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ItemApi {
  private readonly http = inject(HttpClient);

  triggerDownload(podcastId: string, itemId: string) {
    return this.http.post(
      `/api/v1/podcasts/${podcastId}/items/${itemId}/download`,
      null,
      { responseType: 'text' },
    );
  }

  search(input: Signal<ItemSearchInput>): HttpResourceRef<PageHAL<ItemHAL> | undefined> {
    return httpResource<PageHAL<ItemHAL>>(() => {
      const f = input();
      const params: Record<string, string | number> = {
        q: f.q ?? '',
        page: f.page ?? 0,
        size: f.size ?? 12,
        sort: f.sort ?? 'pubDate,DESC',
      };
      if (f.tags && f.tags.length > 0) {
        params['tags'] = f.tags.join(',');
      }
      if (f.status && f.status.length > 0) {
        params['status'] = f.status.join(',');
      }
      return {
        url: '/api/v1/items/search',
        params,
      };
    });
  }
}
