
import {map, switchMap} from 'rxjs/operators';
import {Injectable} from '@angular/core';
import {Actions, Effect} from '@ngrx/effects';
import {Action} from '@ngrx/store';
import {Observable} from 'rxjs/Observable';
import {Podcast} from '../../shared/entity';
import {PodcastService} from '../../shared/service/podcast/podcast.service';
import {FIND_ONE, FindOne, FindOneSuccess} from './podcast.actions';

@Injectable()
export class PodcastEffects {

  @Effect()
  findOne$: Observable<Action> = this.actions$.ofType(FIND_ONE).pipe(
    map((v: FindOne) => v.payload),
    switchMap(id => this.podcastService.findOne(id)),
    map((p: Podcast) => new FindOneSuccess(p))
  );

  constructor(private actions$: Actions, private podcastService: PodcastService) {}

} /* istanbul ignore next */
