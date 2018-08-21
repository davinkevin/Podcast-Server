import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { skip, take } from 'rxjs/operators';
import { Item } from '#app/shared/entity';
import { AppState } from '#app/app.reducer';
import { FindOneAction } from '../item.actions';
import { item } from '../item.reducer';

@Injectable()
export class ItemResolver implements Resolve<Item> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Item> {
		this.store.dispatch(new FindOneAction(route.params.id, route.params.podcastId));

		return this.store.pipe(select(item), skip(1), take(1));
	}
}
