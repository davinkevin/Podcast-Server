import { createFeatureSelector, createSelector } from '@ngrx/store';

import { Item, Podcast } from '../shared/entity';

import { ItemAction, ItemActions } from './item.actions';

export interface ItemState {
	item: Item;
	podcast: Podcast;
}

const initialState: ItemState = {
	item: null,
	podcast: null
};

export function itemReducer(state = initialState, action: ItemActions): ItemState {
	switch (action.type) {
		case ItemAction.FIND_ONE_SUCCESS: {
			return { ...state, item: action.item };
		}

		case ItemAction.FIND_PARENT_PODCAST_SUCCESS: {
			return { ...state, podcast: action.podcast };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<ItemState>('item');
export const item = createSelector(moduleSelector, (s: ItemState) => s.item);
export const podcast = createSelector(moduleSelector, (s: ItemState) => s.podcast);
