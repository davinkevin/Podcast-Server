///<reference path="../../../../node_modules/@ngrx/store/src/store_module.d.ts"/>
import {TestBed, inject, async} from '@angular/core/testing';

import { SearchQueryResolver } from './search-query.resolver';
import {Store, StoreModule} from '@ngrx/store';
import * as fromSearch from '../search.reducer';
import {selectRequest} from '../search.reducer';

describe('SearchQueryResolver', () => {

  let store, resolver;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot([]),
        StoreModule.forFeature('searchModule', {
          search: fromSearch.reducer
        })
      ],
      providers: [SearchQueryResolver]
    });
    resolver = TestBed.get(SearchQueryResolver);
  });

  beforeEach(() => {
    store = TestBed.get(Store);
    spyOn(store, 'dispatch').and.callThrough();
    spyOn(store, 'select').and.callThrough();
  });

  it('should be created', inject([SearchQueryResolver], (service: SearchQueryResolver) => {
    expect(service).toBeTruthy();
  }));

  it('should trigger search if initial response', async(() => {
    /* Given */
    /* When  */
    resolver.resolve(null, null).subscribe(() => {});
    /* Then */
    expect(store.select).toHaveBeenCalledWith(selectRequest);
  }));

});
