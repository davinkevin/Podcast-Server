import { Action } from '@ngrx/store';
import { Item, Page, Pageable, Podcast, uuid } from '../shared/entity';

export enum PodcastAction {
	FIND_ONE = '[Podcast] Find One',
	FIND_ONE_SUCCESS = '[Podcast] Find One Success',
	REFRESH = '[Podcast] Manually refresh podcast',
	REFRESH_SUCCESS = '[Podcast] Manually refresh podcast success',
	FIND_ITEMS = '[Podcast] Find Items for podcast',
	FIND_ITEMS_SUCCESS = '[Podcast] Find Items for podcast success'
}

export class FindOneAction implements Action {
	readonly type = PodcastAction.FIND_ONE;
	constructor(public id: uuid) {}
}

export class FindOneSuccessAction implements Action {
	readonly type = PodcastAction.FIND_ONE_SUCCESS;
	constructor(public podcast: Podcast) {}
}

export class RefreshAction implements Action {
	readonly type = PodcastAction.REFRESH;
	constructor(public podcast: Podcast) {}
}

export class RefreshSuccessAction implements Action {
	readonly type = PodcastAction.REFRESH_SUCCESS;
}

export class FindItemsByPodcastsAndPageAction implements Action {
	readonly type = PodcastAction.FIND_ITEMS;
	constructor(public id: string, public page: Pageable) {}
}

export class FindItemsByPodcastsAndPageSuccessAction implements Action {
	readonly type = PodcastAction.FIND_ITEMS_SUCCESS;
	constructor(public items: Page<Item>) {}
}

export type PodcastActions =
	| FindOneAction
	| FindOneSuccessAction
	| RefreshAction
	| RefreshSuccessAction
	| FindItemsByPodcastsAndPageAction
	| FindItemsByPodcastsAndPageSuccessAction;
