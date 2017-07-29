import 'rxjs/add/operator/map';

import {Injectable} from '@angular/core';
import {Http, URLSearchParams} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {Direction, Item, Page, SearchItemPageRequest} from '../../entity';


@Injectable()
export class ItemService {
  static defaultSearch = {
    page: 0,
    size: 12,
    downloaded: true,
    tags: [],
    sort: [{property: 'pubDate', direction: Direction.DESC}]
  };

  static extendDefaultSearch(params: any): SearchItemPageRequest {
    return {
      page: params.page || ItemService.defaultSearch.page,
      size: params.size || ItemService.defaultSearch.size,
      downloaded: params.downloaded || ItemService.defaultSearch.downloaded,
      tags: params.tags || ItemService.defaultSearch.tags,
      sort: params.sort || ItemService.defaultSearch.sort,
    };
  }

  constructor(private http: Http) {}

  search(searchPageRequest: SearchItemPageRequest = ItemService.defaultSearch): Observable<Page<Item>> {
    const params = this.toParams(searchPageRequest);
    return this.http.get('/api/items/search', {params}).map(res => res.json());
  }

  private toParams(searchPageRequest: SearchItemPageRequest): URLSearchParams {
    // downloaded=true&page=0&size=12&sort=pubDate,DESC&tags=

    const params = new URLSearchParams();
    params.set('page', String(searchPageRequest.page));
    params.set('size', String(searchPageRequest.size));

    params.set('downloaded', String(searchPageRequest.downloaded));
    params.set('sort', searchPageRequest.sort.map(s => `${s.property},${s.direction}`).join(','));
    params.set('tags', searchPageRequest.tags.map(t => t.name).join(','));

    return params;
  }

}
