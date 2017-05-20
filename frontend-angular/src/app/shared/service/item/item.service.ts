import {Injectable} from "@angular/core";
import {Http, URLSearchParams} from "@angular/http";
import {Observable} from "rxjs";
import "rxjs/add/operator/map";
import {Direction, Item, Page, SearchItemPageRequest} from "../../entity";


@Injectable()
export class ItemService {

  private static defaultSearch = {
    page : 0,
    size : 12,
    downloaded : true,
    tags: [],
    sort: [{property: 'pubDate', direction: Direction.ASC}]
  };

  constructor(private http: Http) {}

  search(searchPageRequest: SearchItemPageRequest = ItemService.defaultSearch): Observable<Page<Item>> {
    const params = ItemService.toParams(searchPageRequest);
    return this.http
      .get('/api/items/search', {params})
      .map(res => res.json());
  }

  private static toParams(searchPageRequest: SearchItemPageRequest): URLSearchParams {
    // downloaded=true&page=0&size=12&sort=pubDate,DESC&tags=

    let params = new URLSearchParams();
    params.set('page', String(searchPageRequest.page));
    params.set('size', String(searchPageRequest.size));

    params.set('downloaded', String(searchPageRequest.downloaded));
    params.set('sort', searchPageRequest.sort.map(s => `${s.property},${s.direction}`).join(','));
    params.set('tags', searchPageRequest.tags.join(','));

    return params;
  }
}
