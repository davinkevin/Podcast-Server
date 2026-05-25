import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { queryKeys } from './query-keys';

/**
 * Spring Actuator `info` payload. Only the `git` block is exposed by the
 * backend; `tag` may or may not be present depending on whether the
 * deployed commit is tagged.
 */
export interface BuildInfoHAL {
  readonly git: {
    readonly branch: string;
    readonly commit: {
      readonly id: string;
      readonly time: string;
    };
    readonly tags?: string;
  };
}

@Injectable({ providedIn: 'root' })
export class BuildInfoApi {
  private readonly http = inject(HttpClient);

  /** Spring Actuator `info` — server's git branch, commit id and build
   *  timestamp. Static for a given deployment so we keep it cached
   *  indefinitely. */
  info() {
    return injectQuery(() => ({
      queryKey: queryKeys.buildInfo,
      queryFn: () => lastValueFrom(this.http.get<BuildInfoHAL>('/actuator/info')),
      staleTime: Infinity,
      gcTime: Infinity,
    }));
  }
}
