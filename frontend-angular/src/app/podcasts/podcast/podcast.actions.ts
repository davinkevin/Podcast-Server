
import {Action} from '@ngrx/store';
import {Podcast} from '../../shared/entity';

export const FIND_ONE = '[Podcast] Find One';
export const FIND_ONE_SUCCESS = '[Podcast] Find One Success';

export class FindOne implements Action {
  readonly type = FIND_ONE;
  constructor(public payload: string) {}
}

export class FindOneSuccess implements Action {
  readonly type = FIND_ONE_SUCCESS;
  constructor(public payload: Podcast) {}
}

export type All
  = FindOne
  | FindOneSuccess
  ;
