import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import { queryKeys } from './query-keys';

@Injectable({ providedIn: 'root' })
export class DownloadApi {
  private readonly http = inject(HttpClient);
  private readonly queryClient = inject(QueryClient);

  /** Parallel-download limit. Backend serves it as a plain integer body. */
  limit() {
    return injectQuery(() => ({
      queryKey: queryKeys.downloads.limit(),
      queryFn: () => lastValueFrom(this.http.get<number>('/api/v1/downloads/limit')),
    }));
  }

  updateLimitMutation() {
    return injectMutation(() => ({
      mutationFn: (value: number) =>
        lastValueFrom(this.http.post<number>('/api/v1/downloads/limit', value)),
      onSuccess: () => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.downloads.limit(),
        });
      },
    }));
  }

  // SSE drives queue/downloading state; no client query to invalidate.
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

  // Reorder an item in the download queue. `position` is the zero-based target
  // index. Backend rebroadcasts the new queue via SSE so no client query to
  // invalidate.
  moveInQueue(id: string, position: number) {
    return this.http.post(
      '/api/v1/downloads/queue',
      { id, position },
      { responseType: 'text' },
    );
  }

  // Wipe every WAITING item from the queue in one shot. Downloads currently in
  // progress are left alone. Backend broadcasts the new (empty) queue via SSE.
  emptyQueue() {
    return this.http.delete('/api/v1/downloads/queue', { responseType: 'text' });
  }
}
