import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/add/operator/map';
import {Page, Item, SearchItemPageRequest} from '../../entity';

@Injectable()
export class ItemService {

  constructor(private http: Http) {}

  search(searchPageRequest: SearchItemPageRequest = { page : 0, size : 12, downloaded : null}): Observable<Page<Item>> {
    return this.http
      .post('/api/item/search', searchPageRequest)
      .map(res => res.json());
  }

}
