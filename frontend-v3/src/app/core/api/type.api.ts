import { Injectable } from '@angular/core';
import { httpResource } from '@angular/common/http';

import { TypeContainerHAL } from '../models/type.model';

/**
 * Podcast types are static for the lifetime of the app — backend ships an
 * immutable enum (RSS / Youtube / MyTF1 / FranceTv / Dailymotion / Gulli /
 * upload). We rely on `httpResource`'s lazy fetch + cache: the request signal
 * never changes, so the GET fires once on first read and the result is reused
 * by every consumer.
 */
@Injectable({ providedIn: 'root' })
export class TypeApi {
  readonly types = httpResource<TypeContainerHAL>(() => ({
    url: '/api/v1/podcasts/types',
  }));
}
