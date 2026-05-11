import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { FindPodcastInformationHAL } from '../models/find.model';

@Injectable({ providedIn: 'root' })
export class FindApi {
  private readonly http = inject(HttpClient);

  /**
   * Backend handler reads the body as a plain string, so we POST text/plain
   * with the URL itself as the body — no JSON envelope.
   */
  byUrl(url: string) {
    return this.http.post<FindPodcastInformationHAL>(
      '/api/v1/podcasts/find',
      url,
      { headers: { 'Content-Type': 'text/plain' } },
    );
  }
}
