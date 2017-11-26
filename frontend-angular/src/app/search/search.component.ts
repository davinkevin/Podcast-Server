import {Component, OnInit} from '@angular/core';
import 'rxjs/add/observable/combineLatest';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/map';
import {FormBuilder, FormGroup} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Store} from '@ngrx/store';
import {PageEvent} from '@angular/material';

import * as SearchActions from './search.actions';
import {defaultSearch} from '../shared/service/item/item.service';
import {selectResults} from './search.reducer';
import {Direction, Item, Page, Sort, Status} from '../shared/entity';

interface SearchItemRequestViewModel {
  q?: string;
  page?: number;
  size?: number;
  status?: StatusesValue;
  tags?: string;
  sort?: Sort;
}

export enum StatusesValue {
  ALL, DOWNLOADED, NOT_DOWNLOADED
}


@Component({
  selector: 'ps-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  doNotEmit = {emitEvent: false};

  statuses = [
    {title: "All", value:StatusesValue.ALL},
    {title: "Downloaded", value:StatusesValue.DOWNLOADED},
    {title: "Not Downloaded", value:StatusesValue.NOT_DOWNLOADED}
  ];

  properties = [
    {title: "Relevance", value:"pertinence"},
    {title: "Publication Date", value:"pubDate"},
    {title: "Download Date", value:"downloadDate"}
  ];

  directions = [
    {title: "Descendant", value:Direction.DESC},
    {title: "Ascendant", value:Direction.ASC}
  ];

  form: FormGroup;
  items: Page<Item>;

  constructor(private route: ActivatedRoute, private store: Store<any>, private formBuilder: FormBuilder) {}

  ngOnInit() {
    this.form = this.formBuilder.group({
      q: [''],
      tags: [''],
      page: [],
      size: [],
      status: [StatusesValue.ALL],
      sort: this.formBuilder.group({
        direction: [Direction.DESC],
        property: ['pubDate']
      })
    });

    this.form.valueChanges
      .debounceTime(500)
      .map(v => SearchComponent.toSearchItemRequest(v))
      .subscribe(v => this.search(v));

    this.store.select(selectResults)
      .subscribe(s => this.items = s);

    this.route.data.map(d => d.search)
      .subscribe(s => this.items = s);

    this.route.data.map(d => d.request)
      .subscribe(r => {
        this.form.get('q').setValue(r.q, this.doNotEmit);
        this.form.get('tags').setValue(r.tags.map(t => t.name).join(', '), this.doNotEmit);
        this.form.get('status').setValue(SearchComponent.toStatusesValue(r.status), this.doNotEmit);
        this.form.get('size').setValue(r.size, this.doNotEmit);
        this.form.get('page').setValue(r.page, this.doNotEmit);
        this.form.get('sort').get('direction').setValue(r.sort[0].direction, this.doNotEmit);
        this.form.get('sort').get('property').setValue(r.sort[0].property, this.doNotEmit);
      });
  }

  search(v: any): void {
    this.store.dispatch(new SearchActions.Search({...defaultSearch, ...v }))
  }

  changePage(e: PageEvent) {
    this.form.get('size').setValue(e.pageSize, this.doNotEmit);
    this.form.get('page').setValue(e.pageIndex, this.doNotEmit);
    this.form.updateValueAndValidity({ onlySelf: false, emitEvent: true });
  }

  static toSearchItemRequest(v: SearchItemRequestViewModel) {
    return {
      ...v,
      status: SearchComponent.toStatus(v.status),
      tags: v.tags.split(',').map(t => ({id: t, name: t})),
      sort: [v.sort]
    }
  }

  static toStatusesValue(v: Status[]) {
    if (v.includes(Status.FINISH))
      return StatusesValue.DOWNLOADED;

    if (v.includes(Status.NOT_DOWNLOADED))
      return StatusesValue.NOT_DOWNLOADED;

    return StatusesValue.ALL;
  }

  static toStatus(v: StatusesValue): Status[] {
    if (v === StatusesValue.ALL)
      return [];

    if (v === StatusesValue.DOWNLOADED)
      return [Status.FINISH];

    return [Status.NOT_DOWNLOADED, Status.DELETED, Status.STARTED, Status.STOPPED, Status.PAUSED]
  }
} /* istanbul ignore next */
