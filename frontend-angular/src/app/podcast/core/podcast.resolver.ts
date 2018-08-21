import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import { FindOneAction } from '../podcast.actions';
import { selectPodcast } from '../podcast.reducer';
import { Injectable } from '@angular/core';
import { skip, take } from 'rxjs/operators';
import { Podcast } from '#app/shared/entity';
import { AppState } from '#app/app.reducer';

@Injectable()
export class PodcastResolver implements Resolve<Podcast> {
	constructor(private store: Store<AppState>) {}

	resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast> {
		this.store.dispatch(new FindOneAction(route.params.id));

		return this.store.pipe(select(selectPodcast), skip(1), take(1));
	}
}

export const toPodcast = (d: any) => d.podcast;
