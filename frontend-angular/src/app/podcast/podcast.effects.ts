import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs';
import { concatMap, flatMap, map, switchMap } from 'rxjs/operators';

import { Item, Page, Podcast } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';
import { PodcastService } from '../shared/service/podcast/podcast.service';

import {
	PodcastAction,
	FindItemsByPodcastsAndPageAction,
	FindItemsByPodcastsAndPageSuccessAction,
	FindOneAction,
	FindOneSuccessAction,
	RefreshAction,
	RefreshSuccessAction
} from './podcast.actions';

@Injectable()
export class PodcastEffects {
	@Effect()
	findOne$: Observable<Action> = this.actions$.pipe(
		ofType(PodcastAction.FIND_ONE),
		map((v: FindOneAction) => v.id),
		switchMap(id => this.podcastService.findOne(id)),
		map((p: Podcast) => new FindOneSuccessAction(p))
	);

	@Effect()
	findItemByPodcastAndPage$: Observable<Action> = this.actions$.pipe(
		ofType(PodcastAction.FIND_ITEMS),
		switchMap(({ id, page }: FindItemsByPodcastsAndPageAction) => this.itemService.findByPodcastAndPage(id, page)),
		map((i: Page<Item>) => new FindItemsByPodcastsAndPageSuccessAction(i))
	);

	@Effect()
	refresh: Observable<Action> = this.actions$.pipe(
		ofType(PodcastAction.REFRESH),
		map((a: RefreshAction) => a.podcast),
		concatMap((p: Podcast) => this.podcastService.refresh(p), p => p),
		flatMap(p => [new RefreshSuccessAction(), new FindItemsByPodcastsAndPageAction(p.id)])
	);

	constructor(private actions$: Actions, private podcastService: PodcastService, private itemService: ItemService) {}
}
