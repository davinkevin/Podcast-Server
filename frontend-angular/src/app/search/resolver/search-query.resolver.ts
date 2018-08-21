import { Injectable } from '@angular/core';
import { SearchItemPageRequest } from '#app/shared/entity';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { searchRequest } from '../search.reducer';
import { take } from 'rxjs/operators';
import { AppState } from '#app/app.reducer';

@Injectable()
export class SearchQueryResolver implements Resolve<SearchItemPageRequest> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<SearchItemPageRequest> {
		return this.store.pipe(select(searchRequest), take(1));
	}
}
