import { createFeatureSelector, createSelector } from '@ngrx/store';
import { SearchState } from './search/search.reducer';
import { PodcastsState } from './podcasts/podcasts.reducer';
import { PodcastState } from './podcast/podcast.reducer';
import { ItemState } from './item/item.reducer';
import { AppAction, AppActions } from './app.actions';

export interface AppState {
  search: SearchState;
  podcasts: PodcastsState;
  podcast: PodcastState;
  item: ItemState;
}

export interface State {
  open: boolean;
}

const initialState: State = {
  open: false
};

export function sidenav(state = initialState, action: AppActions): State {
  switch (action.type) {

    case AppAction.OPEN_SIDE_NAV: {
      return {...state, open: true };
    }

    case AppAction.CLOSE_SIDE_NAV: {
      return {...state, open: false };
    }

    default: {return state; }

  }
}

const sideNavFeature = createFeatureSelector('sidenav');
export const selectSideNavOpen = createSelector(sideNavFeature, (s: State) => s.open);
