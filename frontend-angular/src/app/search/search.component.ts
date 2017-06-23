import { Component, OnInit } from '@angular/core';
import {Item, Page} from '../shared/entity';
import {ItemService} from '../shared/service/item/item.service';

@Component({
  selector: 'ps-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  items: Page<Item>;

  constructor(private itemService: ItemService) {}

  ngOnInit() {
    this.itemService
      .search()
      .subscribe(v => this.items = v);
  }

}
