import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { injectQuery } from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { queryKeys } from './query-keys';

@Injectable({ providedIn: 'root' })
export class DownloadApi {
  private readonly http = inject(HttpClient);

  /** Parallel-download limit. Backend serves it as a plain integer body. */
  limit() {
    return injectQuery(() => ({
      queryKey: queryKeys.downloads.limit(),
      queryFn: () => lastValueFrom(this.http.get<number>('/api/v1/downloads/limit')),
    }));
  }

  updateLimit(value: number) {
    return this.http.post<number>('/api/v1/downloads/limit', value);
  }

  stopAll() {
    return this.http.post('/api/v1/downloads/stop', null, { responseType: 'text' });
  }

  stopOne(id: string) {
    return this.http.post(`/api/v1/downloads/${id}/stop`, null, { responseType: 'text' });
  }

  removeFromQueue(id: string, stop = false) {
    return this.http.delete(`/api/v1/downloads/queue/${id}?stop=${stop}`, {
      responseType: 'text',
    });
  }
}
