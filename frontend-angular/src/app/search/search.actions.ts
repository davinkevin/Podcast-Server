import { Action } from '@ngrx/store'
import {Item, Page, SearchItemPageRequest} from '../shared/entity';

export const SEARCH =         '[Items] Search';
export const SEARCH_SUCCESS = '[Items] Search Success';

export class Search implements Action {
  readonly type = SEARCH;
  constructor(public payload: SearchItemPageRequest) {}
}

export class SearchSuccess implements Action {
  readonly type = SEARCH_SUCCESS;

  constructor(public payload: Page<Item>) {}
}

export type All
  = Search
  | SearchSuccess
  ;

