import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { PodcastState } from '../../podcast.reducer';
import { ActivatedRoute } from '@angular/router';
import { map } from 'rxjs/operators';
import { Item, Page } from '../../../shared/entity';
import { toPodcastPageOfItems } from '../podcast-items.resolver';
import { CompanionComponent } from '@davinkevin/companion-component';

@Component({
	selector: 'ps-episodes',
	templateUrl: './episodes.component.html',
	styleUrls: ['./episodes.component.scss']
})
export class EpisodesComponent implements OnInit, OnDestroy {
	items: Page<Item>;
	private companion = new CompanionComponent();

	constructor(private store: Store<PodcastState>, private route: ActivatedRoute) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.route.data.pipe(untilDestroy(), map(toPodcastPageOfItems)).subscribe(v => (this.items = v));
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
