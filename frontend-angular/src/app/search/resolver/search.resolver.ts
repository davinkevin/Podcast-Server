import { Injectable } from '@angular/core';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Item, Page} from '../../shared/entity';
import {Observable} from 'rxjs/Observable';
import {ItemService} from '../../shared/service/item/item.service';

@Injectable()
export class SearchResolver implements Resolve<Page<Item>> {

  constructor(private itemService: ItemService) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Page<Item>> {
    const page = +route.queryParamMap.get('page');
    const params = ItemService.extendDefaultSearch({page});
    return this.itemService.search(params);
  }

}
