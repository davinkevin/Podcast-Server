import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { TypeContainerHAL } from '../models/type.model';
import { queryKeys } from './query-keys';

/**
 * Podcast types are static for the lifetime of the app — the backend derives
 * them from its registered updaters (RSS / Youtube / MyTF1 / FranceTv /
 * Dailymotion / Gulli / Twitch / upload), which cannot change while it runs.
 * Long staleTime so we essentially fetch once per session.
 */
@Injectable({ providedIn: 'root' })
export class TypeApi {
  private readonly http = inject(HttpClient);

  types() {
    return injectQuery(() => ({
      queryKey: queryKeys.types,
      queryFn: () =>
        lastValueFrom(this.http.get<TypeContainerHAL>('/api/v1/podcasts/types')),
      staleTime: Infinity,
    }));
  }
}
