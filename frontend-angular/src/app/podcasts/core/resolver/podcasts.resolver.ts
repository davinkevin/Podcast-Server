import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { Podcast } from '#app/shared/entity';
import { podcasts } from '../../podcasts.reducer';
import * as PodcastsActions from '../../podcasts.actions';
import { skip, take } from 'rxjs/operators';
import { AppState } from '#app/app.reducer';

@Injectable()
export class PodcastsResolver implements Resolve<Podcast[]> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast[]> {
		this.store.dispatch(new PodcastsActions.FindAll());

		return this.store.pipe(select(podcasts), skip(1), take(1));
	}
}
