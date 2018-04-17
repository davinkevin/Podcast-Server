import {Action} from '@ngrx/store';

export enum AppAction {
  OPEN_SIDE_NAV = '[SideNav] Open SideNav',
  CLOSE_SIDE_NAV = '[SideNav] Close SideNav'
}

export class OpenSideNavAction implements Action {
  readonly type = AppAction.OPEN_SIDE_NAV;
}
export class CloseSideNavAction implements Action {
  readonly type = AppAction.CLOSE_SIDE_NAV;
}

export type AppActions
  = OpenSideNavAction
  | CloseSideNavAction
  ;
