import { Action } from '@ngrx/store';
import { Item, uuid } from '../shared/entity';

export enum ItemAction {
	FIND_ONE = '[Item] Find One',
	FIND_ONE_SUCCESS = '[Item] Find One Success'
}

export class FindOneAction implements Action {
	readonly type = ItemAction.FIND_ONE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export class FindOneSuccessAction implements Action {
	readonly type = ItemAction.FIND_ONE_SUCCESS;
	constructor(public item: Item) {}
}

export type ItemActions = FindOneAction | FindOneSuccessAction;
