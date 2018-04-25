import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { select, Store } from '@ngrx/store';

import { AppState } from '../app.reducer';
import { Item } from '../shared/entity';
import { CompanionComponent } from '@davinkevin/companion-component';
import { item } from '#app/item/item.reducer';

@Component({
	selector: 'ps-item',
	templateUrl: './item.component.html',
	styleUrls: ['./item.component.scss']
})
export class ItemComponent implements OnInit, OnDestroy {
	item: Item;

	private companion = new CompanionComponent();

	constructor(private store: Store<AppState>) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.store.pipe(select(item), untilDestroy()).subscribe(v => (this.item = v));
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
