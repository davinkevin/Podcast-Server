import { Injectable } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { AppState } from '#app/app.reducer';
import { Observable } from 'rxjs';
import { skip, take } from 'rxjs/operators';
import { Podcast } from '#app/shared/entity';
import { FindParentPodcastAction } from '#app/item/item.actions';
import { podcast } from '#app/item/item.reducer';

@Injectable()
export class PodcastResolver implements Resolve<Podcast> {
  constructor(private store: Store<AppState>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast> {
    this.store.dispatch(new FindParentPodcastAction(route.params.podcastId));
    return this.store.pipe(select(podcast), skip(1), take(1));
  }
}
