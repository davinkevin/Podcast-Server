import { Action } from '@ngrx/store';
import { Item, Podcast, uuid } from '../shared/entity';

export enum ItemAction {
	FIND_ONE = '[Item] Find One',
	FIND_ONE_SUCCESS = '[Item] Find One Success',
	FIND_PARENT_PODCAST = '[Item] Find parent podcast',
	FIND_PARENT_PODCAST_SUCCESS = '[Item] Find parent podcast Success',
	DELETE = '[Item] Delete item',
	RESET = '[Item] Reset item'
}

export class FindOneAction implements Action {
	readonly type = ItemAction.FIND_ONE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export class FindOneSuccessAction implements Action {
	readonly type = ItemAction.FIND_ONE_SUCCESS;
	constructor(public item: Item) {}
}

export class FindParentPodcastAction implements Action {
	readonly type = ItemAction.FIND_PARENT_PODCAST;
	constructor(public id: uuid) {}
}

export class FindParentPodcastSuccessAction implements Action {
	readonly type = ItemAction.FIND_PARENT_PODCAST_SUCCESS;
	constructor(public podcast: Podcast) {}
}

export class DeleteItemAction implements Action {
	readonly type = ItemAction.DELETE;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export class ResetAction implements Action {
	readonly type = ItemAction.RESET;
	constructor(public itemId: uuid, public podcastId: uuid) {}
}

export type ItemActions =
	| FindOneAction
	| FindOneSuccessAction
	| FindParentPodcastAction
	| FindParentPodcastSuccessAction
	| DeleteItemAction;
