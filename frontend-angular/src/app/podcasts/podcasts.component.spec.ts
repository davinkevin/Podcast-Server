import { DebugElement } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatIconModule, MatToolbarModule } from '@angular/material';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action, Store, StoreModule } from '@ngrx/store';
import { FindAll, FindAllSuccess } from 'app/podcasts/podcasts.actions';
import { cold, hot } from 'jasmine-marbles';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';
import { ReplaySubject } from 'rxjs/ReplaySubject';

import { OpenSideNavAction } from '../app.actions';
import { AppState } from '../app.reducer';
import { PodcastService } from '../shared/service/podcast/podcast.service';
import { ToolbarModule } from '../shared/toolbar/toolbar.module';

import { PodcastsComponent } from './podcasts.component';
import { PodcastsEffects } from './podcasts.effects';
import * as fromPodcasts from './podcasts.reducer';
import { reducer } from './podcasts.reducer';
import { APP_BASE_HREF } from '@angular/common';

describe('PodcastsFeature', () => {
	const podcasts = [
		{
			id: '8ba490ac-8f9a-4e2d-8758-b65e783e783a',
			title: "Comme des poissons dans l'eau",
			type: 'RSS',
			lastUpdate: '2016-01-30T18:01:31.919+01:00',
			cover: {
				id: '26d3b096-e424-42fe-bedc-07943efe2809',
				url: '/api/podcasts/8ba490ac-8f9a-4e2d-8758-b65e783e783a/cover.jpg',
				width: 200,
				height: 200
			}
		},
		{
			id: '238f1309-715f-4910-87f4-159ba4d6885e',
			title: 'Dockercast',
			type: 'RSS',
			lastUpdate: '2017-02-15T22:01:39.854+01:00',
			cover: {
				id: 'bfc3b55b-56bf-4ab7-a446-64fa084b7aa5',
				url: '/api/podcasts/238f1309-715f-4910-87f4-159ba4d6885e/cover.jpg',
				width: 2998,
				height: 2998
			}
		},
		{
			id: 'ead438e2-121e-43c1-bbd5-a86e4b0b612a',
			title: 'Le Porncast',
			type: 'RSS',
			lastUpdate: '2016-08-07T21:02:20.298+02:00',
			cover: {
				id: 'c8c6e659-870f-4797-808d-022e2d6b0cb9',
				url: '/api/podcasts/ead438e2-121e-43c1-bbd5-a86e4b0b612a/cover.jpg',
				width: 200,
				height: 200
			}
		},
		{
			id: '1f964152-2d5a-4b34-9223-8ba383b31d77',
			title: '56 Kast',
			type: 'RSS',
			lastUpdate: '2016-03-26T17:03:51.748+01:00',
			cover: {
				id: 'acbb8fc6-3f94-4903-ad80-57b0edcc9715',
				url: '/api/podcasts/1f964152-2d5a-4b34-9223-8ba383b31d77/cover.jpg',
				width: 200,
				height: 200
			}
		},
		{
			id: '9a58c257-bcb8-4c6d-a70b-f378e6684acb',
			title: 'Ta Gueule',
			type: 'RSS',
			lastUpdate: '2016-08-07T21:02:20.464+02:00',
			cover: {
				id: 'f4414ced-1365-499d-9f54-66d7e21ce27f',
				url: '/api/podcasts/9a58c257-bcb8-4c6d-a70b-f378e6684acb/cover.jpg',
				width: 200,
				height: 200
			}
		},
		{
			id: '17ae402d-545f-4ea7-b924-d52e8b864e5f',
			title: 'Les experts F1',
			type: 'RSS',
			lastUpdate: '2017-04-14T22:01:44.875+02:00',
			cover: {
				id: '95cb0794-3617-41b6-8be5-2d4b5b22214b',
				url: '/api/podcasts/17ae402d-545f-4ea7-b924-d52e8b864e5f/cover.jpg',
				width: 200,
				height: 200
			}
		},
		{
			id: '9af7599a-37d9-418e-9322-a29758eea2b4',
			title: 'Apple - Keynotes',
			type: 'RSS',
			lastUpdate: '2017-06-07T21:02:12.713+02:00',
			cover: {
				id: '3b68edd1-c653-405c-b204-8b8d0bba3c91',
				url: '/api/podcasts/9af7599a-37d9-418e-9322-a29758eea2b4/cover.png',
				width: 200,
				height: 200
			}
		},
		{
			id: '5680d254-57e6-4eb8-80e0-702262e02903',
			title: 'Pokémon',
			type: 'Gulli',
			lastUpdate: '2017-05-21T07:01:19.957+02:00',
			cover: {
				id: 'a0f168b7-149b-4668-bdf2-d7f458950a29',
				url: '/api/podcasts/5680d254-57e6-4eb8-80e0-702262e02903/cover.jpg',
				width: 1080,
				height: 1080
			}
		}
	];

	describe('PodcastsComponent', () => {
		let comp: PodcastsComponent;
		let fixture: ComponentFixture<PodcastsComponent>;
		let el: DebugElement;
		let store: Store<AppState>;

		beforeEach(
			async(() => {
				TestBed.configureTestingModule({
					imports: [
						MatToolbarModule,
						MatIconModule,
						RouterTestingModule,

						ToolbarModule,
						/* NgRx */
						StoreModule.forRoot({}),
						StoreModule.forFeature('podcasts', fromPodcasts.reducer)
					],
					declarations: [PodcastsComponent]
				}).compileComponents();
			})
		);

		beforeEach(() => {
			store = TestBed.get(Store);
			spyOn(store, 'dispatch').and.callThrough();
			spyOn(store, 'select').and.callThrough();
		});

		beforeEach(() => {
			store.dispatch(new FindAllSuccess(podcasts));
			fixture = TestBed.createComponent(PodcastsComponent);
			comp = fixture.componentInstance;
			el = fixture.debugElement;
			fixture.detectChanges();
		});

		it('should be created', () => {
			expect(comp).toBeTruthy();
		});

		it('should init with podcasts from resolver', () => {
			/* Given */
			/* When  */
			fixture.whenStable().then(() => {
				/* Then  */
				const podcastsCards = el.queryAll(By.css('img'));
				expect(podcastsCards.length).toEqual(8);
			});
		});

		it('should open sidenav if click on burger button', () => {
			/* Given */
			const button = el.query(By.css('.toolbar__hamburger'));
			/* When  */
			button.triggerEventHandler('click', null);
			/* Then  */
			expect(store.dispatch).toHaveBeenCalledWith(new OpenSideNavAction());
		});
	});

	describe('PodcastsEffects', () => {
		let effects: PodcastsEffects;
		let podcastService: PodcastService;
		let actions: Observable<Action>;

		beforeEach(() => {
			const findAll = jest.fn().mockReturnValue(of([]));
			podcastService = jest.fn<PodcastService>(() => ({ findAll }))();
		});

		beforeEach(() => {
			actions = new ReplaySubject<Action>(1);
		});

		beforeEach(() => {
			TestBed.configureTestingModule({
				providers: [PodcastsEffects, provideMockActions(() => actions), { provide: PodcastService, useValue: podcastService }]
			}).compileComponents();

			effects = TestBed.get(PodcastsEffects);
		});

		it('should transform FindAll action to FindAllSuccess', () => {
			/* Given */
			actions = hot('--a-', { a: new FindAll() });

			const expected = cold('--b', { b: new FindAllSuccess([]) });

			expect(effects.findAll$).toBeObservable(expected);
			expect(podcastService.findAll).toHaveBeenCalled();
		});
	});

	describe('PodcastsReducers', () => {
		const previousState = { podcasts: [], error: { message: 'foo' } };

		it('should have an initial state', () => {
			/* Given */
			const findAllAction = new FindAll();

			/* When  */
			const state = reducer(undefined, findAllAction);

			/* Then  */
			expect(state).toEqual({ podcasts: [] });
		});

		it('should do nothing when findAllAction triggered', () => {
			/* Given */
			const findAllAction = new FindAll();

			/* When  */
			const state = reducer(previousState, findAllAction);

			/* Then  */
			expect(state).toEqual(previousState);
		});

		it('should change podcasts when findAllSuccess', () => {
			/* Given */
			const findAllSuccessAction = new FindAllSuccess(podcasts);
			/* When  */
			const state = reducer(previousState, findAllSuccessAction);
			/* Then  */
			expect(state.podcasts).toEqual(podcasts);
		});
	});
});
