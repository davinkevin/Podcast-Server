import { Component, OnDestroy, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { PodcastState } from '../../podcast.reducer';
import { Item, Page } from '#app/shared/entity';
import { CompanionComponent } from '@davinkevin/companion-component';
import { selectPodcastItems } from '#app/podcast/podcast.reducer';

@Component({
	selector: 'ps-episodes',
	templateUrl: './episodes.component.html',
	styleUrls: ['./episodes.component.scss']
})
export class EpisodesComponent implements OnInit, OnDestroy {
	items: Page<Item>;
	private companion = new CompanionComponent();

	constructor(private store: Store<PodcastState>) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.store.pipe(
		  select(selectPodcastItems),
      untilDestroy()
    ).subscribe(v => (this.items = v));
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
