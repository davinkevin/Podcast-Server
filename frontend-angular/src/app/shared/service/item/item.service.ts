import 'rxjs/add/operator/map';

import {Injectable} from '@angular/core';
import {Http, URLSearchParams} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {Direction, Item, Page, SearchItemPageRequest} from '../../entity';



@Injectable()
export class ItemService {

  constructor(private http: Http) {}

  search(searchPageRequest: SearchItemPageRequest): Observable<Page<Item>> {
    const params = toParams(searchPageRequest);
    return this.http.get('/api/items/search', {params}).map(res => res.json());
  }

} /* istanbul ignore next */

export function toParams(searchPageRequest: SearchItemPageRequest): URLSearchParams {
  // downloaded=true&page=0&size=12&sort=pubDate,DESC&tags=

  const params = new URLSearchParams();
  params.set('q', searchPageRequest.q);
  params.set('page', String(searchPageRequest.page));
  params.set('size', String(searchPageRequest.size));

  if (searchPageRequest.status && searchPageRequest.status.length > 0) {
    params.set('status', String(searchPageRequest.status));
  }

  params.set('sort', searchPageRequest.sort.map(s => `${s.property},${s.direction}`).join(','));
  params.set('tags', searchPageRequest.tags.map(t => t.name).join(','));

  return params;
}

export const defaultSearch = {
  q: null,
  page: 0,
  size: 12,
  status: [],
  tags: [],
  sort: [{property: 'pubDate', direction: Direction.DESC}]
};
