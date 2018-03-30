import {Injectable} from '@angular/core';
import {Actions, Effect, ofType} from '@ngrx/effects';
import {Action} from '@ngrx/store';
import {Observable} from 'rxjs/Observable';
import {map, switchMap} from 'rxjs/operators';

import {Podcast} from '../shared/entity';
import {PodcastService} from '../shared/service/podcast/podcast.service';

import * as PodcastsActions from './podcasts.actions';


@Injectable()
export class PodcastsEffects {
  @Effect()
  findAll$: Observable<Action> = this.actions$.pipe(
      ofType(PodcastsActions.FIND_ALL), switchMap(() => this.podcastService.findAll()),
      map((results: Podcast[]) => new PodcastsActions.FindAllSuccess(results)));

  constructor(private actions$: Actions, private podcastService: PodcastService) {}
}
