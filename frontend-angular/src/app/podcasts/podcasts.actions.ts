
import {Action} from '@ngrx/store';
import {BackendError, Podcast} from '../shared/entity';

export const FIND_ALL = "[Podcasts] Find all";
export const FIND_ALL_SUCCESS = "[Podcasts] Find all Success";
export const FIND_ALL_ERROR = "[Podcasts] Find all Error";

export class FindAll implements Action {
  readonly type = FIND_ALL;
  readonly payload = null;
}

export class FindAllSuccess implements Action {
  readonly type = FIND_ALL_SUCCESS;

  constructor(public payload: Podcast[]) {}
}

export class FindAllError implements Action {
  readonly type = FIND_ALL_ERROR;

  constructor(public payload: BackendError) {}
}


export type All = FindAll | FindAllSuccess | FindAllError;
