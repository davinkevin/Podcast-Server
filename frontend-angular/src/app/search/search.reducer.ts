import { createFeatureSelector, createSelector } from '@ngrx/store';
import { Direction, Item, Page, SearchItemPageRequest } from '../shared/entity';
import { SearchAction, SearchActions } from './search.actions';

export interface SearchState {
	request: SearchItemPageRequest;
	results: Page<Item>;
}

const initialState: SearchState = {
	request: {
		q: null,
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

export function search(state = initialState, action: SearchActions): SearchState {
	switch (action.type) {
		case SearchAction.SEARCH: {
			return { ...state, request: action.pageRequest };
		}

		case SearchAction.SEARCH_SUCCESS: {
			return { ...state, results: action.results };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<SearchState>('search');
export const searchResults = createSelector(moduleSelector, (s: SearchState) => s.results);
export const searchRequest = createSelector(moduleSelector, (s: SearchState) => s.request);
