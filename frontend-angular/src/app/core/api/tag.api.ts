import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { TagsContainerHAL } from '../models/tag.model';
import { queryKeys } from './query-keys';

export interface TagSearchInput {
  readonly name: string;
}

@Injectable({ providedIn: 'root' })
export class TagApi {
  private readonly http = inject(HttpClient);

  search(input: Signal<TagSearchInput | undefined>) {
    return injectQuery(() => {
      const f = input();
      return {
        queryKey: f ? queryKeys.tags.search(f.name) : ['tags', 'search', 'noop'],
        queryFn: () =>
          lastValueFrom(
            this.http.get<TagsContainerHAL>('/api/v1/tags/search', {
              params: new HttpParams().set('name', f!.name),
            }),
          ),
        // Don't fire when the input is empty — the debounce-style "no query
        // yet" case shouldn't ping the server.
        enabled: !!f,
      };
    });
  }
}
