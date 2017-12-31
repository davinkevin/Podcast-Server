
import {Podcast} from '../shared/entity';
import * as PodcastsActions from './podcasts.actions';
import {createFeatureSelector, createSelector} from '@ngrx/store';

export interface State {
  podcasts: Podcast[];
}

const initialState: State = {
  podcasts: []
};

export function reducer(state = initialState, action: PodcastsActions.All): State {
  switch (action.type) {

    case PodcastsActions.FIND_ALL: {
      return { ...state };
    }

    case PodcastsActions.FIND_ALL_SUCCESS: {
      return {...state, podcasts: action.payload};
    }

    default: {return state; }

  }
}

const moduleSelector = createFeatureSelector<State>('podcasts');
export const selectPodcasts = createSelector(moduleSelector, (s: State) => s.podcasts);
