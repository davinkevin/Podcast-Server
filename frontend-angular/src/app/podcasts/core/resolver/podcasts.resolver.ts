import { Injectable } from '@angular/core';
import {Store} from '@ngrx/store';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import { Podcast } from '../../../shared/entity';
import {selectPodcasts} from '../../podcasts.reducer';
import * as PodcastsActions from '../../podcasts.actions';
import 'rxjs/add/operator/skip';

@Injectable()
export class PodcastsResolver implements Resolve<Podcast[]> {

  constructor(private store: Store<any>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast[]> {
    this.store.dispatch(new PodcastsActions.FindAll());

    return this.store.select(selectPodcasts)
      .skip(1)
      .take(1);
  }
} /* istanbul ignore next */
