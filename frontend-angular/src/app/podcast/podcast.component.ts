import { Location } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { LocationBackAction, RouterNavigateAction } from '@davinkevin/router-store-helper';
import { Store } from '@ngrx/store';
import { map } from 'rxjs/operators';

import { Podcast } from '../shared/entity';

import { toPodcast } from './core/podcast.resolver';
import { RefreshAction } from './podcast.actions';
import { PodcastState } from './podcast.reducer';
import { CompanionComponent } from '@davinkevin/companion-component';

@Component({
	selector: 'ps-podcast',
	templateUrl: './podcast.component.html',
	styleUrls: ['./podcast.component.scss']
})
export class PodcastComponent implements OnInit, OnDestroy {
	podcast: Podcast;
	private companion = new CompanionComponent();

	constructor(private store: Store<PodcastState>, private route: ActivatedRoute, private location: Location) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.route.data.pipe(untilDestroy(), map(toPodcast)).subscribe(v => (this.podcast = v));
	}

	refresh() {
		this.store.dispatch(new RefreshAction(this.podcast));
	}

	back() {
		this.store.dispatch(new RouterNavigateAction(['podcasts']));
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
