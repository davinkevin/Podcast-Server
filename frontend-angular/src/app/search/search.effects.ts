import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { Action, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { concatMap, filter, map, switchMap } from 'rxjs/operators';

import { Item, Page, SearchItemPageRequest } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';
import { Search, SearchAction, SearchSuccess } from './search.actions';
import { AppState } from '#app/app.reducer';
import { AppAction, DownloadProgressAction } from '#app/app.actions';
import { searchRequest } from '#app/search/search.reducer';

@Injectable()
export class SearchEffects {
	@Effect()
	search$: Observable<Action> = this.actions$.pipe(
		ofType(SearchAction.SEARCH),
		map((action: Search) => action.pageRequest),
		switchMap((terms: SearchItemPageRequest) => this.itemService.search(terms)),
		map((results: Page<Item>) => new SearchSuccess(results))
	);

	@Effect()
	updateSearchResultsAfterDownloadComplete = this.actions$.pipe(
		ofType(AppAction.DOWNLOAD_PROGRESS),
		map((a: DownloadProgressAction) => a.item),
		filter((item: Item) => item.isDownloaded),
		concatMap(() => this.store.select(searchRequest)),
		map((sr: SearchItemPageRequest) => new Search(sr))
	);

	constructor(private actions$: Actions, private store: Store<AppState>, private itemService: ItemService) {}
}
