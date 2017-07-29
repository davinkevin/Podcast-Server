import {Component, OnInit} from '@angular/core';
import {Item, Page} from '../shared/entity';
import {ActivatedRoute} from '@angular/router';

@Component({
  selector: 'ps-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {

  items: Page<Item>;

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.data
      .map(d => (<any> d).search)
      .subscribe(s => this.items = s);
  }

}
