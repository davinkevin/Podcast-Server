import { Component, OnInit } from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Podcast} from '../shared/entity';
import {map} from 'rxjs/operators';
import {OpenSideNavAction} from '../app.actions';
import {Store} from '@ngrx/store';
import {AppState} from '../app.reducer';

@Component({
  selector: 'ps-podcasts',
  templateUrl: './podcasts.component.html',
  styleUrls: ['./podcasts.component.scss']
})
export class PodcastsComponent implements OnInit {

  podcasts: Podcast[];

  constructor(private store: Store<AppState>, private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.data.pipe(
      map(d => d.podcasts as Podcast[]),
      map(ps => ps.sort(byDate))
    ).subscribe(d => this.podcasts = d);
  }

  openSideNav() {
    this.store.dispatch(new OpenSideNavAction());
  }

}

function byDate(a: Podcast, b: Podcast) {
  return new Date(b.lastUpdate).getTime() - new Date(a.lastUpdate).getTime();
}
