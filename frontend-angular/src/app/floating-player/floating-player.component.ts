import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AppState } from '#app/app.reducer';
import { select, Store } from '@ngrx/store';
import { display, DisplayState, item } from '#app/floating-player/floating-player.reducer';
import { CompanionComponent } from '@davinkevin/companion-component';
import { Item } from '#app/shared/entity';
import { CloseAction } from '#app/floating-player/floating-player.actions';

@Component({
	selector: 'ps-floating-player',
	templateUrl: './floating-player.component.html',
	styleUrls: ['./floating-player.component.scss']
})
export class FloatingPlayerComponent implements OnInit, OnDestroy {
	companion = new CompanionComponent();

	display: DisplayState;
	_item: Item;

	@ViewChild('video') videoPlayer;
	@ViewChild('audio') audioPlayer;

	constructor(private store: Store<AppState>) {}

	ngOnInit() {
		const untilDestroy = this.companion.untilDestroy();

		this.store.pipe(untilDestroy(), select(item)).subscribe(v => {
			this.item = v;
		});

		this.store.pipe(untilDestroy(), select(display)).subscribe(v => {
			this.display = v;
		});
	}

	get mediaType() {
		if (this.item == null || this.item.mimeType == null) {
			return 'unknown';
		}

		return this.item.mimeType.substr(0, this.item.mimeType.indexOf('/'));
	}

	get item() {
		return this._item;
	}

	set item(i: Item) {
		this._item = i;
		if (this.videoPlayer != null) {
			this.videoPlayer.nativeElement.load();
		}
		if (this.audioPlayer != null) {
			this.audioPlayer.nativeElement.load();
		}
	}

	close() {
		this.store.dispatch(new CloseAction());
	}

	ngOnDestroy(): void {
		this.companion.destroy();
	}
}
