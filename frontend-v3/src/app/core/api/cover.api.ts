import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class CoverApi {
  private readonly http = inject(HttpClient);

  /** Deletes covers no longer referenced by any item, older than `days` (default 365 server-side). */
  cleanup(days: number) {
    return this.http.delete(`/api/v1/covers?days=${days}`, {
      responseType: 'text',
    });
  }
}
