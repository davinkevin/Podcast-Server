import 'rxjs/add/operator/map';

import {Injectable} from '@angular/core';
import {Http, URLSearchParams} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {Direction, Item, Page, SearchItemPageRequest} from '../../entity';
import {HttpClient, HttpParams} from '@angular/common/http';

@Injectable()
export class ItemService {

  constructor(private http: HttpClient) {}

  search(searchPageRequest: SearchItemPageRequest): Observable<Page<Item>> {
    const params = toParams(searchPageRequest);
    return this.http.get<Page<Item>>('/api/items/search', {params});
  }

} /* istanbul ignore next */

export function toParams(request: SearchItemPageRequest): HttpParams {
  // downloaded=true&page=0&size=12&sort=pubDate,DESC&tags=

  let params = new HttpParams()
    .set('q', request.q || '')
    .set('page', String(request.page))
    .set('size', String(request.size))
    .set('sort', request.sort.map(s => `${s.property},${s.direction}`).join(','))
    .set('tags', request.tags.map(t => t.name).join(','));

  if (request.status && request.status.length > 0) {
    params = params.set('status', String(request.status));
  }

  return params;
}

export const defaultSearch: SearchItemPageRequest = {
  q: null,
  page: 0,
  size: 12,
  status: [],
  tags: [],
  sort: [{property: 'pubDate', direction: Direction.DESC}]
};
