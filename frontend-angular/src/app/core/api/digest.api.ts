import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { DigestHAL } from '../models/digest.model';
import { queryKeys } from './query-keys';

export interface DigestInput {
  /** ISO-8601 duration window, e.g. `PT24H`, `P7D`, `P30D`. */
  readonly within: string;
  readonly maxItemsPerPodcast?: number;
}

const DEFAULT_MAX_ITEMS_PER_PODCAST = 12;

@Injectable({ providedIn: 'root' })
export class DigestApi {
  private readonly http = inject(HttpClient);

  get(input: Signal<DigestInput>) {
    return injectQuery(() => {
      const f = input();
      const maxItems = f.maxItemsPerPodcast ?? DEFAULT_MAX_ITEMS_PER_PODCAST;
      return {
        queryKey: queryKeys.digest.within(f.within, maxItems),
        queryFn: () => {
          const params = new HttpParams()
            .set('within', f.within)
            .set('maxItemsPerPodcast', maxItems);
          return lastValueFrom(
            this.http.get<DigestHAL>('/api/v1/digest', { params }),
          );
        },
      };
    });
  }
}
