import {Action} from '@ngrx/store';

export const OPEN_SIDE_NAV = '[SideNav] Open SideNav';
export const CLOSE_SIDE_NAV = '[SideNav] Close SideNav';

export class OpenSideNavAction implements Action {
  readonly type = OPEN_SIDE_NAV;
}
export class CloseSideNavAction implements Action {
  readonly type = CLOSE_SIDE_NAV;
}

export type all
  = OpenSideNavAction
  | CloseSideNavAction
  ;
