import { createFeatureSelector, createSelector } from '@ngrx/store';

import { Podcast } from '../shared/entity';

import { PodcastsActions, PodcastsAction } from './podcasts.actions';

export interface PodcastsState {
	podcasts: Podcast[];
}

const initialState: PodcastsState = {
	podcasts: []
};

export function reducer(state = initialState, action: PodcastsActions): PodcastsState {
	switch (action.type) {
		case PodcastsAction.FIND_ALL: {
			return { ...state };
		}

		case PodcastsAction.FIND_ALL_SUCCESS: {
			return { ...state, podcasts: action.payload };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<PodcastsState>('podcasts');
export const selectPodcasts = createSelector(moduleSelector, (s: PodcastsState) => s.podcasts);
