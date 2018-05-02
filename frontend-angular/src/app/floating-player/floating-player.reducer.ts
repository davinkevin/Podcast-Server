import { Item } from '#app/shared/entity';
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { FloatingPlayerAction, FloatingPlayerActions } from '#app/floating-player/floating-player.actions';

export type DisplayState = 'OPENED' | 'CLOSED';

export interface FloatingPlayerState {
	item: Item;
	display: DisplayState;
}

const initialState: FloatingPlayerState = {
	item: null,
	display: 'CLOSED'
};

export function floatingPlayer(state = initialState, action: FloatingPlayerActions): FloatingPlayerState {
	switch (action.type) {
		case FloatingPlayerAction.PLAY: {
			return { ...state, item: action.item, display: 'OPENED' };
		}

		case FloatingPlayerAction.CLOSE: {
			return { ...state, item: null, display: 'CLOSED' };
		}

		default: {
			return state;
		}
	}
}

const moduleSelector = createFeatureSelector<FloatingPlayerState>('floatingPlayer');
export const item = createSelector(moduleSelector, (s: FloatingPlayerState) => s.item);
export const display = createSelector(moduleSelector, (s: FloatingPlayerState) => s.display);
