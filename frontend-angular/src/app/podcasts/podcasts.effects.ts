import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { Podcast } from '../shared/entity';
import { PodcastService } from '../shared/service/podcast/podcast.service';

import { FindAllSuccess, PodcastsAction } from './podcasts.actions';

@Injectable()
export class PodcastsEffects {
	@Effect()
	findAll$: Observable<Action> = this.actions$.pipe(
		ofType(PodcastsAction.FIND_ALL),
		switchMap(() => this.podcastService.findAll()),
		map((results: Podcast[]) => new FindAllSuccess(results))
	);

	constructor(private actions$: Actions, private podcastService: PodcastService) {}
}
