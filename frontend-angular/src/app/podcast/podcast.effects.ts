import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { map, switchMap } from 'rxjs/operators';

import { Item, Page, Podcast } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';
import { PodcastService } from '../shared/service/podcast/podcast.service';

import {
	FIND_ITEMS,
	FIND_ONE,
	FindItemsByPodcastsAndPageAction,
	FindItemsByPodcastsAndPageSuccessAction,
	FindOneAction,
	FindOneSuccessAction,
	REFRESH,
	RefreshAction,
	RefreshSuccessAction
} from './podcast.actions';

@Injectable()
export class PodcastEffects {
	@Effect()
	findOne$: Observable<Action> = this.actions$.pipe(
		ofType(FIND_ONE),
		map((v: FindOneAction) => v.payload),
		switchMap(id => this.podcastService.findOne(id)),
		map((p: Podcast) => new FindOneSuccessAction(p))
	);

	@Effect()
	findItemByPodcastAndPage$: Observable<Action> = this.actions$.pipe(
		ofType(FIND_ITEMS),
		switchMap(({ id, page }: FindItemsByPodcastsAndPageAction) => this.itemService.findByPodcastAndPage(id, page)),
		map((i: Page<Item>) => new FindItemsByPodcastsAndPageSuccessAction(i))
	);

	@Effect()
	refresh: Observable<Action> = this.actions$.pipe(
		ofType(REFRESH),
		map((a: RefreshAction) => a.payload),
		switchMap(p => this.podcastService.refresh(p)),
		map(_ => new RefreshSuccessAction())
	);

	constructor(private actions$: Actions, private podcastService: PodcastService, private itemService: ItemService) {}
}
