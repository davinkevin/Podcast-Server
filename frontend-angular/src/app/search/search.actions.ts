import { Action } from '@ngrx/store';
import { Item, Page, SearchItemPageRequest } from '../shared/entity';

export enum SearchAction {
	SEARCH = '[Items] Search',
	SEARCH_SUCCESS = '[Items] Search Success'
}

export class Search implements Action {
	readonly type = SearchAction.SEARCH;
	constructor(public pageRequest: SearchItemPageRequest) {}
}

export class SearchSuccess implements Action {
	readonly type = SearchAction.SEARCH_SUCCESS;

	constructor(public results: Page<Item>) {}
}

export type SearchActions = Search | SearchSuccess;
