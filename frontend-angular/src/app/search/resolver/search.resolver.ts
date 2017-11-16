import {Injectable} from '@angular/core';
import 'rxjs/add/operator/take';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/filter';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Item, Page} from '../../shared/entity';
import {Observable} from 'rxjs/Observable';
import {Store} from '@ngrx/store';
import * as SearchActions from '../search.actions';
import {selectResults} from '../search.reducer';
import {ItemService} from '../../shared/service/item/item.service';

@Injectable()
export class SearchResolver implements Resolve<Page<Item>> {

  constructor(private store: Store<any>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Page<Item>> {
    return this.store
      .select(selectResults)
      .do(d => this.updateIfEmpty(d))
      .filter((data: Page<any>) => data.totalElements > -1)
      .take(1);
  }

  updateIfEmpty(data: Page<any>) {
    if (data.totalElements === -1) {
      this.store.dispatch(new SearchActions.Search(ItemService.defaultSearch));
    }
  }

} /* istanbul ignore next */
