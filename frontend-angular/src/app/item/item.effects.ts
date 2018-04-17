import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { Observable } from 'rxjs/Observable';
import { map, switchMap } from 'rxjs/operators';

import { Item } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';

import { ItemAction, FindOneAction, FindOneSuccessAction } from './item.actions';

@Injectable()
export class ItemEffects {
	@Effect()
	findOne$: Observable<Action> = this.actions$.pipe(
		ofType(ItemAction.FIND_ONE),
		switchMap((a: FindOneAction) => this.itemService.findById(a.itemId, a.podcastId)),
		map((i: Item) => new FindOneSuccessAction(i))
	);

	constructor(private actions$: Actions, private itemService: ItemService) {}
}
