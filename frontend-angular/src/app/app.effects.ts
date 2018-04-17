import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { map } from 'rxjs/operators';
import { CloseSideNavAction } from './app.actions';
import { ROUTER_NAVIGATION } from '@ngrx/router-store';

@Injectable()
export class AppEffects {

  @Effect()
  closePanelOnRouteChange$ = this.actions$.pipe(
    ofType(ROUTER_NAVIGATION),
    map(() => new CloseSideNavAction())
  );

  constructor(private actions$: Actions) {}
}
