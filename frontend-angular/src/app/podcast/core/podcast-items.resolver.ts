import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { select, Store } from '@ngrx/store';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { selectPodcastItems } from '../podcast.reducer';
import { skip, take } from 'rxjs/operators';
import { Direction, Item, Page } from '#app/shared/entity';
import { FindItemsByPodcastsAndPageAction } from '../podcast.actions';
import { AppState } from '#app/app.reducer';

@Injectable()
export class PodcastItemsResolver implements Resolve<Page<Item>> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Page<Item>> {
		this.store.dispatch(
			new FindItemsByPodcastsAndPageAction(route.params.id)
		);

		return this.store.pipe(select(selectPodcastItems), skip(1), take(1));
	}
}
