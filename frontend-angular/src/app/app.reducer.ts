import * as AppAction from './app.actions';
import {createFeatureSelector, createSelector} from '@ngrx/store';


export interface State {
  open: boolean;
}

const initialState: State = {
  open: false
};

export function sidenav(state = initialState, action: AppAction.all): State {
  switch (action.type) {

    case AppAction.OPEN_SIDE_NAV: {
      return {...state, open: true };
    }

    case AppAction.CLOSE_SIDE_NAV: {
      return {...state, open: false };
    }

    default: {return state; }

  }
}

const sideNavFeature = createFeatureSelector('sidenav');
export const selectSideNavOpen = createSelector(sideNavFeature, (s: State) => s.open);
