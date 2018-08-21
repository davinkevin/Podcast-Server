import { Injectable } from '@angular/core';
import { map, skip, take } from 'rxjs/operators';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Item, Page } from '#app/shared/entity';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { searchRequest, searchResults } from '../search.reducer';
import { AppState } from '#app/app.reducer';
import { Search } from '#app/search/search.actions';

@Injectable()
export class SearchResolver implements Resolve<Page<Item>> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Page<Item>> {
	  this.store.pipe(select(searchRequest), map(r => new Search(r)))
      .subscribe(v => this.store.dispatch(v));

		return this.store.pipe(select(searchResults), skip(1), take(1));
	}
}
