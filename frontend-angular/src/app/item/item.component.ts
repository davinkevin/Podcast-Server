import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Store} from '@ngrx/store';
import {map} from 'rxjs/operators';

import {AppState} from '../app.reducer';
import {ComponentDestroyCompanion} from '../shared/component.utils';
import {Item} from '../shared/entity';

import {toItem} from './core/item.resolver';

@Component({
  selector: 'ps-item',
  templateUrl: './item.component.html',
  styleUrls: ['./item.component.scss']
})
export class ItemComponent implements OnInit, OnDestroy {
  item: Item;
  showPlayer = false;

  companion = new ComponentDestroyCompanion();

  constructor(private store: Store<AppState>, private route: ActivatedRoute) {}

  ngOnInit() {
    const untilDestroy = this.companion.untilDestroy();

    this.route.data.pipe(untilDestroy(), map(toItem)).subscribe(item => this.item = item)
  }

  togglePlayer() {
    this.showPlayer = !this.showPlayer;
  }

  get mediaType() {
    if (this.item.mimeType == null) {
      return 'unknown';
    }

    return this.item.mimeType.substr(0, this.item.mimeType.indexOf('/'))
  }

  ngOnDestroy(): void {
    this.companion.destroy();
  }
}
