
import {map, switchMap} from 'rxjs/operators';
import {Injectable} from '@angular/core';
import {Actions, Effect} from '@ngrx/effects';
import {Action} from '@ngrx/store';
import {Observable} from 'rxjs/Observable';
import {Podcast} from '../../shared/entity';;
import {FIND_ONE, FindOne, FindOneSuccess, REFRESH, RefreshAction, RefreshSuccessAction} from './podcast.actions';
import {PodcastService} from '../shared/service/podcast/podcast.service';

@Injectable()
export class PodcastEffects {

  @Effect()
  findOne$: Observable<Action> = this.actions$.ofType(FIND_ONE).pipe(
    map((v: FindOne) => v.payload),
    switchMap(id => this.podcastService.findOne(id)),
    map((p: Podcast) => new FindOneSuccess(p))
  );

  @Effect()
  refresh: Observable<Action> = this.actions$.ofType(REFRESH).pipe(
    map((a: RefreshAction) => a.payload),
    switchMap(p => this.podcastService.refresh(p)),
    map(_ => new RefreshSuccessAction())
  );

  constructor(private actions$: Actions, private podcastService: PodcastService) {}

} /* istanbul ignore next */
