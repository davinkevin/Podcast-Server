import {BackendError, Direction, Item, Page, SearchItemPageRequest} from '../shared/entity';
import * as SearchActions from './search.actions';

export interface ModuleState {
  searchModule: {
    search: State;
  }
}

export interface State {
  request: SearchItemPageRequest;
  results: Page<Item>,
  error: BackendError
}

const initialState: State = {
  request: {
    page: 0, size: 12, status: [], tags: [],
    sort: [{property: 'pubDate', direction: Direction.DESC}]
  },
  results: {
    content: [],
    first: true, last: true,
    totalPages: 0, totalElements: -1, numberOfElements: 0,
    size: 0, number: 0,
    sort: [{direction: Direction.DESC, property: 'pubDate'}]
  },
  error: {
    message: 'empty'
  }
};

export function reducer(state = initialState, action: SearchActions.All): State {
  switch (action.type) {

    case SearchActions.SEARCH: {
      return {...state, request: action.payload};
    }

    case SearchActions.SEARCH_SUCCESS: {
      return {...state, results: action.payload};
    }

    case SearchActions.SEARCH_ERROR: {
      return {...state, error: action.payload};
    }

    default: { return state; }

  }
}

export function selectResults(s: ModuleState) {
  return s.searchModule.search.results;
}

export function selectRequest(s: ModuleState) {
  return s.searchModule.search.request;
}
