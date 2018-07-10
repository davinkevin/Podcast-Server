import {Action} from '@ngrx/store';
import { Item, uuid } from '#app/shared/entity';

export enum AppAction {
  OPEN_SIDE_NAV = '[SideNav] Open SideNav',
  CLOSE_SIDE_NAV = '[SideNav] Close SideNav',
  DOWNLOAD_ITEM = '[Download] Download item',
  DOWNLOAD_PROGRESS = '[Download] Download progressing',
}

export class OpenSideNavAction implements Action {
  readonly type = AppAction.OPEN_SIDE_NAV;
}
export class CloseSideNavAction implements Action {
  readonly type = AppAction.CLOSE_SIDE_NAV;
}
export class DownloadItemAction implements Action {
  readonly type = AppAction.DOWNLOAD_ITEM;
  constructor(public itemId: uuid, public podcastId: uuid) {}
}
export class DownloadProgressAction implements Action {
  readonly type = AppAction.DOWNLOAD_PROGRESS;
  constructor(public item: Item) {}
}

export type AppActions
  = OpenSideNavAction
  | CloseSideNavAction
  | DownloadProgressAction
  ;
