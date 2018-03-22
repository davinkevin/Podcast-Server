import {Action} from '@ngrx/store';

export const OPEN_SIDE_NAV = '[SideNav] Open SideNav';
export const CLOSE_SIDE_NAV = '[SideNav] Close SideNav';

export class OpenSideNavAction implements Action {
  readonly type = OPEN_SIDE_NAV;
}
export class CloseSideNavAction implements Action {
  readonly type = CLOSE_SIDE_NAV;
}

export const LOCATION_BACK = '[LOCATION] back';
export class LocationBackAction implements Action {
  readonly type = LOCATION_BACK;
}

export type all
  = OpenSideNavAction
  | CloseSideNavAction
  | LocationBackAction
  ;
