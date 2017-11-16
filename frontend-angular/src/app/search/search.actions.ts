import { Action } from '@ngrx/store'
import {BackendError, Item, Page, SearchItemPageRequest} from '../shared/entity';

export const SEARCH =         '[Items] Search';
export const SEARCH_SUCCESS = '[Items] Search Success';
export const SEARCH_ERROR =   '[Items] Search Error';

export class Search implements Action {
  readonly type = SEARCH;
  constructor(public payload: SearchItemPageRequest) {}
}

export class SearchSuccess implements Action {
  readonly type = SEARCH_SUCCESS;

  constructor(public payload: Page<Item>) {}
}

export class SearchError implements Action {
  readonly type = SEARCH_ERROR;

  constructor(public payload: BackendError) {}
}

export type All
  = Search
  | SearchSuccess
  | SearchError;

