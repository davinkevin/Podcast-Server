import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { concatMap, map, switchMap } from 'rxjs/operators';
import { CloseSideNavAction } from './app.actions';
import { ROUTER_NAVIGATION } from '@ngrx/router-store';
import { AppAction, DownloadItemAction } from '#app/app.actions';
import { ItemService } from '#app/shared/service/item/item.service';

@Injectable()
export class AppEffects {

  @Effect()
  closePanelOnRouteChange$ = this.actions$.pipe(
    ofType(ROUTER_NAVIGATION),
    map(() => new CloseSideNavAction())
  );

  @Effect({ dispatch: false })
  downloadItem = this.actions$.pipe(
    ofType(AppAction.DOWNLOAD_ITEM),
    concatMap(({itemId, podcastId}: DownloadItemAction) => this.itemService.download(itemId, podcastId)),
  );

  constructor(private actions$: Actions, private itemService: ItemService) {}
}
