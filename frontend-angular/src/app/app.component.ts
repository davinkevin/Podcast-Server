import {Component, OnInit} from '@angular/core';
import {select, Store} from '@ngrx/store';
import {AppState, selectSideNavOpen} from './app.reducer';
import {CloseSideNavAction} from './app.actions';

@Component({
  selector: 'ps-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  sideNavOpen = false;

  constructor(private store: Store<AppState>) {}

  ngOnInit(): void {
    this.store.pipe(
      select(selectSideNavOpen)
    ).subscribe(v => this.sideNavOpen = v);
  }

  onOpenChange($event: boolean) {
    if ($event === true) {
      return;
    }

    this.store.dispatch(new CloseSideNavAction());
  }
}
