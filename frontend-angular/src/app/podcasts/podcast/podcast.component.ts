import {Component, OnDestroy, OnInit} from '@angular/core';
import {Podcast} from '../../shared/entity';
import {Store} from '@ngrx/store';
import {ActivatedRoute} from '@angular/router';
import {toPodcast} from './core/podcast.resolver';
import {map} from 'rxjs/operators';
import {Location} from '@angular/common';
import {RefreshAction} from './podcast.actions';
import {OpenSideNavAction} from '../../app.actions';
import {ComponentDestroyCompanion} from '../../shared/component.utils';
import {PodcastState} from './podcast.reducer';

@Component({
  selector: 'ps-podcast',
  templateUrl: './podcast.component.html',
  styleUrls: ['./podcast.component.scss']
})
export class PodcastComponent implements OnInit, OnDestroy {

  podcast: Podcast;
  private companion: ComponentDestroyCompanion;

  constructor(private store: Store<PodcastState>, private route: ActivatedRoute, private location: Location) {}

  ngOnInit() {

    this.companion = new ComponentDestroyCompanion();
    const untilDestroy = this.companion.untilDestroy();

    this.route.data.pipe(
      untilDestroy(),
      map(toPodcast)
    ).subscribe(v => this.podcast = v);
  }

  goBack() {
    this.location.back();
  }

  refresh() {
    this.store.dispatch(new RefreshAction(this.podcast));
  }

  openSideNav() {
    this.store.dispatch(new OpenSideNavAction());
  }


  ngOnDestroy(): void {
    this.companion.destroy();
  }
}
