import {map, switchMap} from 'rxjs/operators';
import {Injectable} from '@angular/core';
import {Actions, Effect, ofType} from '@ngrx/effects';
import {Observable} from 'rxjs/Observable';
import {Action} from '@ngrx/store';

import {ItemService} from '../shared/service/item/item.service';
import * as SearchActions from './search.actions';
import {Item, Page, SearchItemPageRequest} from '../shared/entity';

@Injectable()
export class SearchEffects {

  @Effect()
  search$: Observable<Action> = this.actions$
    .pipe(
      ofType(SearchActions.SEARCH),
      map((action: SearchActions.Search) => action.payload),
      switchMap((terms: SearchItemPageRequest) => this.itemService.search(terms)),
      map((results: Page<Item>) => new SearchActions.SearchSuccess(results))
    );

  constructor(private actions$: Actions, private itemService: ItemService) {}

}
