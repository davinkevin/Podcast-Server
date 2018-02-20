import {Component, OnDestroy, OnInit} from '@angular/core';
import {Store} from '@ngrx/store';
import {PodcastState} from '../../podcast.reducer';
import {ActivatedRoute} from '@angular/router';
import {map} from 'rxjs/operators';
import {ComponentDestroyCompanion} from '../../../../shared/component.utils';
import {Item, Page} from '../../../../shared/entity';
import {toPodcastPageOfItems} from '../podcast-items.resolver';

@Component({
  selector: 'ps-episodes',
  templateUrl: './episodes.component.html',
  styleUrls: ['./episodes.component.scss']
})
export class EpisodesComponent implements OnInit, OnDestroy {

  items: Page<Item>;
  private companion: ComponentDestroyCompanion;

  constructor(private store: Store<PodcastState>, private route: ActivatedRoute) { }

  ngOnInit() {
    this.companion = new ComponentDestroyCompanion();
    const untilDestroy = this.companion.untilDestroy();

    this.route.data.pipe(
      untilDestroy(),
      map(toPodcastPageOfItems)
    ).subscribe(v => this.items = v);
  }

  ngOnDestroy(): void {
    this.companion.destroy();
  }
}
