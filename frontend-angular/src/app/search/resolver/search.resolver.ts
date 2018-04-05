import { Injectable } from '@angular/core';
import { skip, take } from 'rxjs/operators';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Item, Page } from '../../shared/entity';
import { Observable } from 'rxjs/Observable';
import { select, Store } from '@ngrx/store';
import * as SearchActions from '../search.actions';
import { selectResults } from '../search.reducer';
import { defaultSearch } from '../../shared/service/item/item.service';
import { AppState } from '../../app.reducer';

@Injectable()
export class SearchResolver implements Resolve<Page<Item>> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Page<Item>> {
		this.store.dispatch(new SearchActions.Search(defaultSearch));

		return this.store.pipe(select(selectResults), skip(1), take(1));
	}
}
