import { Injectable } from '@angular/core';
import {SearchItemPageRequest} from '../../shared/entity';
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Store} from '@ngrx/store';
import {selectRequest} from '../search.reducer';

@Injectable()
export class SearchQueryResolver implements Resolve<SearchItemPageRequest> {

  constructor(private store: Store<any>) {}

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<SearchItemPageRequest> {
    return this.store.select(selectRequest).take(1);
  }

} /* istanbul ignore next */
