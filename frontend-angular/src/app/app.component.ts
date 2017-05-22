import {Component, OnInit} from '@angular/core';
import {ItemService} from './shared/service/item/item.service';
import {Item, Page} from './shared/entity';

@Component({
  selector: 'ps-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  items: Page<Item>;

  constructor(private itemService: ItemService) {}

  ngOnInit(): void {
    this.itemService.search()
      .subscribe(p => this.items = p);
  }
}
