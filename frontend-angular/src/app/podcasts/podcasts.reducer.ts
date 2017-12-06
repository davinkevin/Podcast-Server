
import {BackendError, Podcast} from '../shared/entity';
import * as PodcastsActions from './podcasts.actions';


export interface ModuleState {
  podcastsModule: {
    podcasts: State;
  }
}

export interface State {
  podcasts: Podcast[];
  error: BackendError;
}

const initialState: State = {
  podcasts: [],
  error: {message: 'empty'}
};

export function reducer(state = initialState, action: PodcastsActions.All): State {
  switch (action.type) {

    case PodcastsActions.FIND_ALL: {
      return { ...state };
    }

    case PodcastsActions.FIND_ALL_SUCCESS: {
      return {...state, podcasts: action.payload};
    }

    case PodcastsActions.FIND_ALL_ERROR: {
      return {...state, error: action.payload};
    }

    default: {return state; }

  }
}



export function selectPodcasts(s: ModuleState) {
  return s.podcastsModule.podcasts.podcasts;
}
