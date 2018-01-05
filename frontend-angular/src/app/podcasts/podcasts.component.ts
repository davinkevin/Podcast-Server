import { Component, OnInit } from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Podcast} from '../shared/entity';
import {map} from 'rxjs/operators';

@Component({
  selector: 'ps-podcasts',
  templateUrl: './podcasts.component.html',
  styleUrls: ['./podcasts.component.scss']
})
export class PodcastsComponent implements OnInit {

  podcasts: Podcast[];

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.data.pipe(
      map(d => d.podcasts as Podcast[])
    ).subscribe(d => this.podcasts = d);
  }

} /* istanbul ignore next */
