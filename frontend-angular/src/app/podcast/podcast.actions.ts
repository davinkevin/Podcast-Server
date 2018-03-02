
import {Action} from '@ngrx/store';
import {Item, Page, Pageable, Podcast} from '../shared/entity';

export const FIND_ONE = '[Podcast] Find One';
export const FIND_ONE_SUCCESS = '[Podcast] Find One Success';
export const REFRESH = '[Podcast] Manually refresh podcast';
export const REFRESH_SUCCESS = '[Podcast] Manually refresh podcast success';
export const FIND_ITEMS = '[Podcast] Find Items for podcast';
export const FIND_ITEMS_SUCCESS = '[Podcast] Find Items for podcast success';

export class FindOneAction implements Action {
  readonly type = FIND_ONE;
  constructor(public payload: string) {}
}

export class FindOneSuccessAction implements Action {
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

export class FindItemsByPodcastsAndPageAction implements Action {
  readonly type = FIND_ITEMS;
  constructor(public id: string, public page: Pageable) {}
}

export class FindItemsByPodcastsAndPageSuccessAction implements Action {
  readonly type = FIND_ITEMS_SUCCESS;
  constructor(public items: Page<Item>) {}
}

export type All
  = FindOneAction
  | FindOneSuccessAction
  | RefreshAction
  | RefreshSuccessAction
  | FindItemsByPodcastsAndPageAction
  | FindItemsByPodcastsAndPageSuccessAction
  ;
