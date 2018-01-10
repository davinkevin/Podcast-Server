
import {Action} from '@ngrx/store';
import {Podcast} from '../../shared/entity';

export const FIND_ONE = '[Podcast] Find One';
export const FIND_ONE_SUCCESS = '[Podcast] Find One Success';
export const REFRESH = '[Podcast] Manually refresh podcast';
export const REFRESH_SUCCESS = '[Podcast] Manually refresh podcast success';

export class FindOne implements Action {
  readonly type = FIND_ONE;
  constructor(public payload: string) {}
}

export class FindOneSuccess implements Action {
  readonly type = FIND_ONE_SUCCESS;
  constructor(public payload: Podcast) {}
}

export class RefreshAction implements Action {
  readonly type = REFRESH;
  constructor(public payload: Podcast) {}
}

export class RefreshSuccessAction implements Action {
  readonly type = REFRESH_SUCCESS;
}

export type All
  = FindOne
  | FindOneSuccess
  | RefreshAction
  | RefreshSuccessAction
  ;
