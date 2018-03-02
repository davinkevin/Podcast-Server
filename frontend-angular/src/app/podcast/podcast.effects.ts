
import {map, switchMap} from 'rxjs/operators';
import {Injectable} from '@angular/core';
import {Actions, Effect} from '@ngrx/effects';
import {Action} from '@ngrx/store';
import {Observable} from 'rxjs/Observable';
import {Item, Page, Podcast} from '../shared/entity';
import {
  FIND_ITEMS, FIND_ONE, FindItemsByPodcastsAndPageAction, FindItemsByPodcastsAndPageSuccessAction, FindOneAction,
  FindOneSuccessAction, REFRESH, RefreshAction,
  RefreshSuccessAction
} from './podcast.actions';
import {PodcastService} from '../shared/service/podcast/podcast.service';
import {ItemService} from '../shared/service/item/item.service';

@Injectable()
export class PodcastEffects {

  @Effect()
  findOne$: Observable<Action> = this.actions$.ofType(FIND_ONE).pipe(
    map((v: FindOneAction) => v.payload),
    switchMap(id => this.podcastService.findOne(id)),
    map((p: Podcast) => new FindOneSuccessAction(p))
  );

  @Effect()
  findItemByPodcastAndPage$: Observable<Action> = this.actions$.ofType(FIND_ITEMS).pipe(
    switchMap(({id, page}: FindItemsByPodcastsAndPageAction) => this.itemService.findByPodcastAndPage(id, page)),
    map((i: Page<Item>) => new FindItemsByPodcastsAndPageSuccessAction(i))
  );

  @Effect()
  refresh: Observable<Action> = this.actions$.ofType(REFRESH).pipe(
    map((a: RefreshAction) => a.payload),
    switchMap(p => this.podcastService.refresh(p)),
    map(_ => new RefreshSuccessAction())
  );

  constructor(private actions$: Actions,
              private podcastService: PodcastService,
              private itemService: ItemService
  ) {}

} /* istanbul ignore next */
