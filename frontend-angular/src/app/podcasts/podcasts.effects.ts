
import {Injectable} from '@angular/core';
import {Actions, Effect} from '@ngrx/effects';
import {Observable} from 'rxjs/Observable';
import * as PodcastsActions from './podcasts.actions';
import {Action} from '@ngrx/store';
import {Podcast} from '../shared/entity';
import {PodcastService} from '../shared/service/podcast/podcast.service';


@Injectable()
export class PodcastsEffects {

  constructor(private actions$: Actions, private podcastService: PodcastService) {}

  @Effect()
  findAll$: Observable<Action> = this.actions$.ofType(PodcastsActions.FIND_ALL)
    .switchMap(() => this.podcastService.findAll())
    .map((results: Podcast[]) => new PodcastsActions.FindAllSuccess(results));

} /* istanbul ignore next */
