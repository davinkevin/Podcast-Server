import { Injectable, Signal } from '@angular/core';
import { httpResource, HttpResourceRef } from '@angular/common/http';

import { TagsContainerHAL } from '../models/tag.model';

export interface TagSearchInput {
  readonly name: string;
}

@Injectable({ providedIn: 'root' })
export class TagApi {
  search(input: Signal<TagSearchInput | undefined>): HttpResourceRef<TagsContainerHAL | undefined> {
    return httpResource<TagsContainerHAL>(() => {
      const f = input();
      if (!f) return undefined;
      return {
        url: '/api/v1/tags/search',
        params: { name: f.name },
      };
    });
  }
}
