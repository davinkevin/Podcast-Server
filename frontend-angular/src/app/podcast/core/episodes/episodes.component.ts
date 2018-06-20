import { Component, OnDestroy, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { PodcastState } from '../../podcast.reducer';
import { Direction, Item, Page, Podcast } from '#app/shared/entity';
import { CompanionComponent } from '@davinkevin/companion-component';
import { selectPodcast, selectPodcastItems } from '#app/podcast/podcast.reducer';
import { PageEvent } from '@angular/material';
import { FindItemsByPodcastsAndPageAction } from '#app/podcast/podcast.actions';

@Component({
	selector: 'ps-episodes',
	templateUrl: './episodes.component.html',
	styleUrls: ['./episodes.component.scss']
})
export class EpisodesComponent implements OnInit, OnDestroy {

  items: Page<Item>;
  podcast: Podcast;

	private companion = new CompanionComponent();

	constructor(private store: Store<PodcastState>) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.store.pipe(
		  select(selectPodcast)
    ).subscribe(p => this.podcast = p);

		this.store.pipe(
		  select(selectPodcastItems),
      untilDestroy()
    ).subscribe(v => (this.items = v));
	}

  changePage(e: PageEvent) {
	  this.store.dispatch(new FindItemsByPodcastsAndPageAction(this.podcast.id, {
	    page: e.pageIndex,
      size: e.pageSize,
      sort: [{ property: 'pubDate', direction: Direction.DESC }]
	  }));
  }

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
