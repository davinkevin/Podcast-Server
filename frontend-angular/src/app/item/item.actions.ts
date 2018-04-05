import { Action } from '@ngrx/store';
import { Item, uuid } from '../shared/entity';

export const FIND_ONE = '[Item] Find One';
export const FIND_ONE_SUCCESS = '[Item] Find One Success';

export class FindOneAction implements Action {
	readonly type = FIND_ONE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export class FindOneSuccessAction implements Action {
	readonly type = FIND_ONE_SUCCESS;
	constructor(public item: Item) {}
}

export type All = FindOneAction | FindOneSuccessAction;
