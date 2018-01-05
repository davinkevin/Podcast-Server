import {Podcast} from '../../../../shared/entity';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Store} from '@ngrx/store';
import {FindOne} from '../../podcast.actions';
import {selectPodcast} from '../../podcast.reducer';
import {Injectable} from '@angular/core';
import {skip, take} from 'rxjs/operators';


@Injectable()
export class PodcastResolver implements Resolve<Podcast> {

  constructor(private store: Store<any>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Podcast> {
    this.store.dispatch(new FindOne(route.params.id));

    return this.store.select(selectPodcast).pipe(
      skip(1),
      take(1)
    );
  }
}

export const toPodcast = (d: any) => d.podcast;
