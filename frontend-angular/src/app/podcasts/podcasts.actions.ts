import { Action } from '@ngrx/store';
import { Podcast } from '../shared/entity';

export enum PodcastsAction {
	FIND_ALL = '[Podcasts] Find all',
	FIND_ALL_SUCCESS = '[Podcasts] Find all Success'
}

export class FindAll implements Action {
	readonly type = PodcastsAction.FIND_ALL;
	readonly payload = null;
}

export class FindAllSuccess implements Action {
	readonly type = PodcastsAction.FIND_ALL_SUCCESS;

	constructor(public payload: Podcast[]) {}
}

export type PodcastsActions = FindAll | FindAllSuccess;
