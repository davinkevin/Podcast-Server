import {Component, OnDestroy, OnInit} from '@angular/core';
import {Podcast} from '../shared/entity';
import {Store} from '@ngrx/store';
import {ActivatedRoute} from '@angular/router';
import {toPodcast} from './core/podcast.resolver';
import {map} from 'rxjs/operators';
import {Location} from '@angular/common';
import {RefreshAction} from './podcast.actions';
import {ComponentDestroyCompanion} from '../shared/component.utils';
import {PodcastState} from './podcast.reducer';
import {LocationBackAction} from '@davinkevin/router-store-helper';

@Component({
  selector: 'ps-podcast',
  templateUrl: './podcast.component.html',
  styleUrls: ['./podcast.component.scss']
})
export class PodcastComponent implements OnInit, OnDestroy {

  podcast: Podcast;
  private companion = new ComponentDestroyCompanion();

  constructor(private store: Store<PodcastState>, private route: ActivatedRoute, private location: Location) {}

  ngOnInit() {
    const untilDestroy = this.companion.untilDestroy();

    this.route.data.pipe(
      untilDestroy(),
      map(toPodcast)
    ).subscribe(v => this.podcast = v);
  }

  refresh() {
    this.store.dispatch(new RefreshAction(this.podcast));
  }

  back() {
    this.store.dispatch(new LocationBackAction());
  }


  ngOnDestroy(): void {
    this.companion.destroy();
  }
}
