import { createFeatureSelector, createSelector } from '@ngrx/store';

import { Direction, Item, Page, SearchItemPageRequest } from '../shared/entity';

import * as SearchActions from './search.actions';

export interface SearchState {
	request: SearchItemPageRequest;
	results: Page<Item>;
}

const initialState: SearchState = {
	request: {
		page: 0,
		size: 12,
		status: [],
		tags: [],
		sort: [{ property: 'pubDate', direction: Direction.DESC }]
	},
	results: {
		content: [],
		first: true,
		last: true,
		totalPages: 0,
		totalElements: -1,
		numberOfElements: 0,
		size: 0,
		number: 0,
		sort: [{ direction: Direction.DESC, property: 'pubDate' }]
	}
};

export function reducer(state = initialState, action: SearchActions.All): SearchState {
	switch (action.type) {
		case SearchActions.SEARCH: {
			return { ...state, request: action.payload };
		}

		case SearchActions.SEARCH_SUCCESS: {
			return { ...state, results: action.payload };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<SearchState>('search');
export const selectResults = createSelector(moduleSelector, (s: SearchState) => s.results);
export const selectRequest = createSelector(moduleSelector, (s: SearchState) => s.request);
