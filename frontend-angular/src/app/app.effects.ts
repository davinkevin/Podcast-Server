import { Injectable } from '@angular/core';
import { Actions, Effect, ofType, ROOT_EFFECTS_INIT } from '@ngrx/effects';
import { concatMap, map } from 'rxjs/operators';
import { CloseSideNavAction } from './app.actions';
import { ROUTER_NAVIGATION } from '@ngrx/router-store';
import { AppAction, DownloadItemAction, DownloadProgressAction } from '#app/app.actions';
import { ItemService } from '#app/shared/service/item/item.service';
import { StompService } from '@davinkevin/ngx-stomp';
import { Item } from '#app/shared/entity';

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

  @Effect()
  downloadWs = this.actions$.pipe(
    ofType(ROOT_EFFECTS_INIT),
    concatMap(() => this.stompService.on<Item>('/topic/download')),
    map((item: Item) => new DownloadProgressAction(item))
  );

  constructor(private actions$: Actions, private itemService: ItemService, private stompService: StompService) {}
}
