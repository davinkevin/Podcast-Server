import { Action } from '@ngrx/store';
import { Item, uuid } from '../shared/entity';
import { AppAction } from '#app/app.actions';

export enum ItemAction {
	FIND_ONE = '[Item] Find One',
	FIND_ONE_SUCCESS = '[Item] Find One Success',
	DELETE = '[Item] Delete item'
}

export class FindOneAction implements Action {
	readonly type = ItemAction.FIND_ONE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export class FindOneSuccessAction implements Action {
	readonly type = ItemAction.FIND_ONE_SUCCESS;
	constructor(public item: Item) {}
}

export class DeleteItemAction implements Action {
	readonly type = ItemAction.DELETE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export type ItemActions = FindOneAction | FindOneSuccessAction | DeleteItemAction;
