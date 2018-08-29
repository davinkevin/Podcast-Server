import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { PageEvent } from '@angular/material';
import { select, Store } from '@ngrx/store';
import { debounceTime, map } from 'rxjs/operators';
import { AppState } from '../app.reducer';
import { Direction, Item, Page, SearchItemPageRequest, Sort, Status } from '../shared/entity';
import { defaultSearch } from '../shared/service/item/item.service';

import { Search } from './search.actions';
import { searchRequest, searchResults } from './search.reducer';
import { CompanionComponent } from '@davinkevin/companion-component';
import { PlayAction } from '#app/floating-player/floating-player.actions';
import { isDownloadable as IsDownloadable, isPlayable as IsPlayable } from '#app/shared/service/item/item.service';
import { DownloadItemAction } from '#app/app.actions';

interface SearchItemRequestViewModel {
	q?: string;
	page?: number;
	size?: number;
	status?: StatusesViewValue;
	tags?: string;
	sort?: Sort;
}

export enum StatusesViewValue {
	ALL,
	DOWNLOADED,
	NOT_DOWNLOADED
}

const DO_NOT_EMIT = { emitEvent: false };

@Component({
	selector: 'ps-search',
	templateUrl: './search.component.html',
	styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit, OnDestroy {
	statuses = [
		{ title: 'All', value: StatusesViewValue.ALL },
		{ title: 'Downloaded', value: StatusesViewValue.DOWNLOADED },
		{ title: 'Not Downloaded', value: StatusesViewValue.NOT_DOWNLOADED }
	];

	properties = [
		{ title: 'Relevance', value: 'pertinence' },
		{ title: 'Publication Date', value: 'pubDate' },
		{ title: 'Download Date', value: 'downloadDate' }
	];

	directions = [{ title: 'Descendant', value: Direction.DESC }, { title: 'Ascendant', value: Direction.ASC }];

	form: FormGroup;
	items: Page<Item>;

	isDownloadable: (item: Item) => boolean = IsDownloadable;
	isPlayable: (item: Item) => boolean = IsPlayable;

	companion = new CompanionComponent();

	constructor(private store: Store<AppState>, private fb: FormBuilder) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.form = this.fb.group({
			q: [''],
			tags: [''],
			page: [],
			size: [],
			status: [],
			sort: this.fb.group({ direction: [], property: [] })
		});

		this.form.valueChanges.pipe(untilDestroy(), debounceTime(500), map(toSearchItemRequest)).subscribe(v => this.search(v));

		this.store.pipe(select(searchResults), untilDestroy()).subscribe(s => (this.items = s));

		this.store.pipe(select(searchRequest), untilDestroy()).subscribe(r => {
      this.form.get('q').setValue(r.q, DO_NOT_EMIT);
			this.form.get('tags').setValue(r.tags.map(t => t.name).join(', '), DO_NOT_EMIT);
			this.form.get('status').setValue(toStatusesValue(r.status), DO_NOT_EMIT);
			this.form.get('size').setValue(r.size, DO_NOT_EMIT);
			this.form.get('page').setValue(r.page, DO_NOT_EMIT);
			this.form
				.get('sort')
				.get('direction')
				.setValue(r.sort[0].direction, DO_NOT_EMIT);
			this.form
				.get('sort')
				.get('property')
				.setValue(r.sort[0].property, DO_NOT_EMIT);
		});
	}

	search(v: SearchItemPageRequest): void {
		this.store.dispatch(new Search({ ...defaultSearch, ...v }));
	}

	changePage(e: PageEvent) {
		this.form.get('size').setValue(e.pageSize, DO_NOT_EMIT);
		this.form.get('page').setValue(e.pageIndex, DO_NOT_EMIT);
		this.form.updateValueAndValidity({ onlySelf: false, emitEvent: true });
	}

	play(item: Item) {
		this.store.dispatch(new PlayAction(item));
	}

	download(item: Item): void {
		this.store.dispatch(new DownloadItemAction(item.id, item.podcastId));
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}

function toStatus(v: StatusesViewValue): Status[] {
	switch (v) {
		case StatusesViewValue.ALL:
			return [];
		case StatusesViewValue.DOWNLOADED:
			return [Status.FINISH];
		default:
			return [Status.NOT_DOWNLOADED, Status.DELETED, Status.STARTED, Status.STOPPED, Status.PAUSED, Status.FAILED];
	}
}

function toStatusesValue(v: Status[]): StatusesViewValue {
	if (v.includes(Status.FINISH)) {
		return StatusesViewValue.DOWNLOADED;
	}

	if (v.includes(Status.NOT_DOWNLOADED)) {
		return StatusesViewValue.NOT_DOWNLOADED;
	}

	return StatusesViewValue.ALL;
}

function toSearchItemRequest(v: SearchItemRequestViewModel): SearchItemPageRequest {
	return {
		...v,
		status: toStatus(v.status),
		tags: v.tags.split(',').map(t => ({ id: t, name: t })),
		sort: [v.sort]
	};
}
