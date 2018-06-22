import { DebugElement } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule } from '@angular/material';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { LocationBackAction, RouterNavigateAction } from '@davinkevin/router-store-helper';
import { Store, StoreModule } from '@ngrx/store';
import { of } from 'rxjs/observable/of';

import { AppState } from '../app.reducer';
import { ToolbarModule } from '../shared/toolbar/toolbar.module';

import { PodcastComponent } from './podcast.component';
import * as fromPodcast from './podcast.reducer';

const PODCAST = {
	id: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed',
	title: 'Jamie Oliver Food Tube',
	url: 'https://www.youtube.com/user/JamieOliver',
	type: 'Youtube',
	lastUpdate: '2018-01-07T14:04:06.191+01:00',
	cover: {
		id: '0f36acc0-0222-4269-a486-37fa9fa700da',
		url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/cover.jpg',
		width: 200,
		height: 200
	},
	description: null,
	hasToBeDeleted: true,
	tags: [
		{ id: '40d93e9f-e769-4a40-b1c7-7493acb3ae99', name: 'Cuisine' },
		{ id: 'e469bb7f-2b04-4903-a2d0-eacc8eea97b2', name: 'Vie-Quotidienne' }
	]
};

describe('PodcastComponent', () => {
	let component: PodcastComponent;
	let fixture: ComponentFixture<PodcastComponent>;
	let el: DebugElement;
	let store: Store<AppState>;

	beforeEach(
		async(() => {
			TestBed.configureTestingModule({
				declarations: [PodcastComponent],
				imports: [
					MatIconModule,
					MatButtonModule,
					MatMenuModule,
					MatToolbarModule,

					ToolbarModule,

					StoreModule.forRoot({}),
					StoreModule.forFeature('podcast', fromPodcast.reducer),

					RouterTestingModule
				],
				providers: [{ provide: ActivatedRoute, useValue: { data: of({ podcast: PODCAST }) } }]
			}).compileComponents();
		})
	);

	beforeEach(async () => {
		store = TestBed.get(Store);
		spyOn(store, 'dispatch').and.callThrough();
	});

	beforeEach(async () => {
		fixture = TestBed.createComponent(PodcastComponent);
		component = fixture.componentInstance;
		el = fixture.debugElement;
		fixture.detectChanges();
		await fixture.whenStable();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should have title in the nav-bar', () => {
		/* Given */
		/* When  */
		const title = el.query(By.css('.toolbar__title'));
		/* Then  */
		expect(asText(title)).toEqual('Jamie Oliver Food Tube');
	});

	it('should have a cover displayed', async () => {
		/* Given */
		/* When  */
		const jumbotron = el.query(By.css('.jumbotron'));
		/* Then  */
		expect(jumbotron.styles['background-image']).toEqual('url(/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/cover.jpg)');
	});

	it('should dispatch action to go back in history', () => {
		/* Given */
		const back = el.query(By.css('.buttons mat-icon'));
		/* When  */
		back.triggerEventHandler('click', null);
		/* Then  */
		expect(store.dispatch).toHaveBeenCalledWith(new RouterNavigateAction(['podcasts']));
	});

	function asText(d: DebugElement) {
		return d.nativeElement.textContent.trim();
	}
});
