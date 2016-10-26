import {Component, OnInit} from '@angular/core';
import {ItemService} from "./shared/item/item.service";
import {Page, Item} from "./shared/entity";

@Component({
  selector: 'ps-root',
  templateUrl: './ps.component.html',
  styleUrls: ['./ps.component.scss']
})
export class PsComponent implements OnInit {

  itemsPage: Page<Item>;

  constructor(private itemService: ItemService) { }

  ngOnInit(): void {
    this.itemService
      .search()
      .subscribe(page => this.itemsPage = page);
  }
}
