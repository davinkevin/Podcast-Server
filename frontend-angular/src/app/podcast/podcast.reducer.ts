import { createFeatureSelector, createSelector } from '@ngrx/store';

import { Item, Page, Podcast } from '../shared/entity';

import { PodcastActions, PodcastAction } from './podcast.actions';

export interface PodcastState {
	podcast: Podcast;
	items: Page<Item>;
}

const initialState: PodcastState = {
	podcast: null,
	items: null
};

export function reducer(state = initialState, action: PodcastActions): PodcastState {
	switch (action.type) {
		case PodcastAction.FIND_ONE_SUCCESS: {
			return { ...state, podcast: action.podcast };
		}

		case PodcastAction.FIND_ITEMS_SUCCESS: {
			return { ...state, items: action.items };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<PodcastState>('podcast');
export const selectPodcast = createSelector(moduleSelector, (s: PodcastState) => s.podcast);
export const selectPodcastItems = createSelector(moduleSelector, (s: PodcastState) => s.items);
