import { Injectable } from '@angular/core';
import {SearchItemPageRequest} from '../../shared/entity';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {select, Store} from '@ngrx/store';
import {selectRequest} from '../search.reducer';
import {take} from 'rxjs/operators';
import {AppState} from '../../app.reducer';

@Injectable()
export class SearchQueryResolver implements Resolve<SearchItemPageRequest> {

  constructor(private store: Store<AppState>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<SearchItemPageRequest> {
    return this.store.pipe(
      select(selectRequest),
      take(1)
    );
  }

}
