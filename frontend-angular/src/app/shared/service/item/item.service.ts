import 'rxjs/add/operator/map';

import {Injectable} from '@angular/core';
import {Http, URLSearchParams} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {Direction, Item, Page, SearchItemPageRequest} from '../../entity';


@Injectable()
export class ItemService {
  static defaultSearch = {
    q: null,
    page: 0,
    size: 12,
    status: undefined,
    tags: [],
    sort: [{property: 'pubDate', direction: Direction.DESC}]
  };

  constructor(private http: Http) {}

  static toParams(searchPageRequest: SearchItemPageRequest): URLSearchParams {
    // downloaded=true&page=0&size=12&sort=pubDate,DESC&tags=

    const params = new URLSearchParams();
    params.set('q', searchPageRequest.q);
    params.set('page', String(searchPageRequest.page));
    params.set('size', String(searchPageRequest.size));

    if (searchPageRequest.status) {
      params.set('status', String(searchPageRequest.status));
    }

    params.set('sort', searchPageRequest.sort.map(s => `${s.property},${s.direction}`).join(','));
    params.set('tags', searchPageRequest.tags.map(t => t.name).join(','));

    return params;
  }

  search(searchPageRequest: SearchItemPageRequest): Observable<Page<Item>> {
    const params = ItemService.toParams(searchPageRequest);
    return this.http.get('/api/items/search', {params}).map(res => res.json());
  }

} /* istanbul ignore next */
