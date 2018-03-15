import { Injectable } from '@angular/core';
import {select, Store} from '@ngrx/store';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import { Podcast } from '../../../shared/entity';
import {selectPodcasts} from '../../podcasts.reducer';
import * as PodcastsActions from '../../podcasts.actions';
import {skip, take} from 'rxjs/operators';
import {AppState} from '../../../app.reducer';

@Injectable()
export class PodcastsResolver implements Resolve<Podcast[]> {

  constructor(private store: Store<AppState>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast[]> {
    this.store.dispatch(new PodcastsActions.FindAll());

    return this.store
      .pipe(
        select(selectPodcasts),
        skip(1),
        take(1)
      );
  }
} /* istanbul ignore next */
