import { Component, OnInit } from '@angular/core';
import { OpenSideNavAction } from '#app/app.actions';
import { AppState } from '#app/app.reducer';
import { Store } from '@ngrx/store';

@Component({
	selector: 'ps-toolbar',
	templateUrl: './toolbar.component.html',
	styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit {
	constructor(private store: Store<AppState>) {}

	ngOnInit() {}

	openSideNav() {
		this.store.dispatch(new OpenSideNavAction());
	}
}
