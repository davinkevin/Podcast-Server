import {Component, OnInit} from '@angular/core';
import {Podcast} from '../../shared/entity';
import {Store} from '@ngrx/store';
import {ActivatedRoute} from '@angular/router';
import {toPodcast} from './core/resolver/podcast.resolver';
import {map} from 'rxjs/operators';
import {Location} from '@angular/common';
import {RefreshAction} from './podcast.actions';

@Component({
  selector: 'ps-podcast',
  templateUrl: './podcast.component.html',
  styleUrls: ['./podcast.component.scss']
})
export class PodcastComponent implements OnInit {

  podcast: Podcast;

  constructor(private store: Store<any>, private route: ActivatedRoute, private location: Location) {}

  ngOnInit() {
    this.route.data.pipe(
      map(toPodcast)
    ).subscribe(v => this.podcast = v);
  }

  goBack() {
    this.location.back();
  }

  refresh() {
    this.store.dispatch(new RefreshAction(this.podcast));
  }

}
