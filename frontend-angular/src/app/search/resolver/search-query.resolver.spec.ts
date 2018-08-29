///<reference path="../../../../node_modules/@ngrx/store/src/store_module.d.ts"/>
import { inject, TestBed } from '@angular/core/testing';

import { SearchQueryResolver } from './search-query.resolver';
import { Store, StoreModule } from '@ngrx/store';
import * as fromSearch from '../search.reducer';

describe('SearchQueryResolver', () => {
	let store, resolver;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [StoreModule.forRoot([]), StoreModule.forFeature('search', fromSearch.search)],
			providers: [SearchQueryResolver]
		});
		resolver = TestBed.get(SearchQueryResolver);
	});

	beforeEach(() => {
		store = TestBed.get(Store);
		spyOn(store, 'dispatch').and.callThrough();
		spyOn(store, 'select').and.callThrough();
	});

	it(
		'should be created',
		inject([SearchQueryResolver], (service: SearchQueryResolver) => {
			expect(service).toBeTruthy();
		})
	);
});
