import {Item, Page, Podcast} from '../../shared/entity';
import * as PodcastActions from './podcast.actions';
import {createFeatureSelector, createSelector} from '@ngrx/store';

export interface PodcastState {
  podcast: Podcast;
  items: Page<Item>;
}

const initialState: PodcastState = {
  podcast: null,
  items: null
};

export function reducer(state = initialState, action: PodcastActions.All): PodcastState {
  switch (action.type) {

    case PodcastActions.FIND_ONE_SUCCESS: {
      return {...state, podcast: action.payload};
    }

    case PodcastActions.FIND_ITEMS_SUCCESS: {
      return {...state, items: action.items};
    }

    default: {return state; }

  }
}

const moduleSelector = createFeatureSelector<PodcastState>('podcast');
export const selectPodcast = createSelector(moduleSelector, (s: PodcastState) => s.podcast);
export const selectPodcastItems = createSelector(moduleSelector, (s: PodcastState) => s.items);
