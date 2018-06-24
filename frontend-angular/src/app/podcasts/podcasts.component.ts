import { Component, OnInit } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { map } from 'rxjs/operators';

import { AppState } from '../app.reducer';
import { Podcast } from '../shared/entity';
import { podcasts } from '#app/podcasts/podcasts.reducer';

@Component({
	selector: 'ps-podcasts',
	templateUrl: './podcasts.component.html',
	styleUrls: ['./podcasts.component.scss']
})
export class PodcastsComponent implements OnInit {
	podcasts: Podcast[];

	constructor(private store: Store<AppState>) {}

	ngOnInit() {
		this.store.pipe(select(podcasts), map(toPodcastOrderedByDate)).subscribe(d => (this.podcasts = d));
	}
}

function toPodcastOrderedByDate(p: Podcast[]) {
	return p.sort((a: Podcast, b: Podcast) => new Date(b.lastUpdate).getTime() - new Date(a.lastUpdate).getTime());
}
