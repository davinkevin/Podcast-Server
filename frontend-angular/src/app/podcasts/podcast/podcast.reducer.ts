
import {Podcast} from '../../shared/entity';
import * as PodcastActions from './podcast.actions';
import {createFeatureSelector, createSelector} from '@ngrx/store';

export interface State {
  podcast: Podcast;
}

const initialState: State = {
  podcast: null
};

export function reducer(state = initialState, action: PodcastActions.All): State {
  switch (action.type) {

    case PodcastActions.FIND_ONE: {
      return { ...state };
    }

    case PodcastActions.FIND_ONE_SUCCESS: {
      return {...state, podcast: action.payload};
    }

    default: {return state; }

  }
}

const moduleSelector = createFeatureSelector<State>('podcast');
export const selectPodcast = createSelector(moduleSelector, (s: State) => s.podcast);
