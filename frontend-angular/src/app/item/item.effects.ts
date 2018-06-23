import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { concatMap, map } from 'rxjs/operators';

import { Item, Podcast } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';

import {
	DeleteItemAction,
	FindOneAction,
	FindOneSuccessAction,
	FindParentPodcastAction,
	FindParentPodcastSuccessAction,
	ItemAction
} from './item.actions';
import { RouterNavigateAction } from '@davinkevin/router-store-helper';
import { PodcastService } from '#app/shared/service/podcast/podcast.service';

@Injectable()
export class ItemEffects {
	@Effect()
	findOne$: Observable<Action> = this.actions$.pipe(
		ofType(ItemAction.FIND_ONE),
		concatMap(({ itemId, podcastId }: FindOneAction) => this.itemService.findById(itemId, podcastId)),
		map((i: Item) => new FindOneSuccessAction(i))
	);

	@Effect()
	findParentPodcast$: Observable<Action> = this.actions$.pipe(
		ofType(ItemAction.FIND_PARENT_PODCAST),
		concatMap(({ id }: FindParentPodcastAction) => this.podcastService.findOne(id)),
		map((p: Podcast) => new FindParentPodcastSuccessAction(p))
	);

	@Effect()
	deleteItem = this.actions$.pipe(
		ofType(ItemAction.DELETE),
		concatMap(({ itemId, podcastId }: DeleteItemAction) => this.itemService.delete(itemId, podcastId), id => id),
		map(podcastId => new RouterNavigateAction(['podcasts', podcastId]))
	);

	constructor(private actions$: Actions, private itemService: ItemService, private podcastService: PodcastService) {}
}
