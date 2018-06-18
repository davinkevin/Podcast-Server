import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { concatMap, map, mapTo } from 'rxjs/operators';

import { Item } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';

import { DeleteItemAction, FindOneAction, FindOneSuccessAction, ItemAction } from './item.actions';
import { RouterNavigateAction } from '@davinkevin/router-store-helper';

@Injectable()
export class ItemEffects {
	@Effect()
	findOne$: Observable<Action> = this.actions$.pipe(
		ofType(ItemAction.FIND_ONE),
		concatMap(({ itemId, podcastId }: FindOneAction) => this.itemService.findById(itemId, podcastId)),
		map((i: Item) => new FindOneSuccessAction(i))
	);

	@Effect()
	deleteItem = this.actions$.pipe(
		ofType(ItemAction.DELETE),
		concatMap(({ itemId, podcastId }: DeleteItemAction) => this.itemService.delete(itemId, podcastId), id => id),
		map(podcastId => new RouterNavigateAction(['podcasts', podcastId]))
	);

	constructor(private actions$: Actions, private itemService: ItemService) {}
}
