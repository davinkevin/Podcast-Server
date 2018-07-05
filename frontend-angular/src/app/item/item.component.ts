import { Component, OnDestroy, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';

import { AppState } from '../app.reducer';
import { Item, Podcast } from '../shared/entity';
import { CompanionComponent } from '@davinkevin/companion-component';
import { item, podcast } from '#app/item/item.reducer';
import { LocationBackAction } from '@davinkevin/router-store-helper';
import { PlayAction } from '#app/floating-player/floating-player.actions';
import { DownloadItemAction } from '#app/app.actions';
import { isDownloadable as IsDownloadable, isPlayable as IsPlayable } from '#app/shared/service/item/item.service';
import { DeleteItemAction, ResetAction } from '#app/item/item.actions';

@Component({
	selector: 'ps-item',
	templateUrl: './item.component.html',
	styleUrls: ['./item.component.scss']
})
export class ItemComponent implements OnInit, OnDestroy {
	item: Item;
	podcast: Podcast;

	isDownloadable: (item: Item) => boolean = IsDownloadable;
	isPlayable: (item: Item) => boolean = IsPlayable;

	private companion = new CompanionComponent();

	constructor(private store: Store<AppState>) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.store.pipe(select(item), untilDestroy()).subscribe(v => (this.item = v));
		this.store.pipe(select(podcast), untilDestroy()).subscribe(v => (this.podcast = v));
	}

	play(): void {
		this.store.dispatch(new PlayAction(this.item));
	}

	delete(): void {
		this.store.dispatch(new DeleteItemAction(this.item.id, this.item.podcastId));
	}

	download(): void {
		this.store.dispatch(new DownloadItemAction(this.item.id, this.item.podcastId));
	}

	reset(): void {
		this.store.dispatch(new ResetAction(this.item.id, this.item.podcastId));
	}

	back(): void {
		this.store.dispatch(new LocationBackAction());
	}

	isEmpty(s: string) {
		return s == null || s === '';
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
