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
export class CoverApi {
  private readonly http = inject(HttpClient);
  private readonly queryClient = inject(QueryClient);

  /** Deletes covers no longer referenced by any item, older than `days`. */
  cleanupMutation() {
    return injectMutation(() => ({
      mutationFn: (days: number) =>
        lastValueFrom(
          this.http.delete(`/api/v1/covers?days=${days}`, { responseType: 'text' }),
        ),
    }));
  }

  /** Default retention (days) for covers on disk. */
  daysToSave() {
    return injectQuery(() => ({
      queryKey: queryKeys.covers.daysToSave(),
      queryFn: () =>
        lastValueFrom(this.http.get<number>('/api/v1/covers/days-to-save')),
    }));
  }

  updateDaysToSaveMutation() {
    return injectMutation(() => ({
      mutationFn: (value: number) =>
        lastValueFrom(
          this.http.post<number>('/api/v1/covers/days-to-save', value),
        ),
      onSuccess: () => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.covers.daysToSave(),
        });
      },
    }));
  }
}
