import { PodcastResolver } from './podcast.resolver';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Store, StoreModule } from '@ngrx/store';
import { async, inject, TestBed } from '@angular/core/testing';
import { reducer } from '../podcast.reducer';
import { FindOneAction, FindOneSuccessAction } from '../podcast.actions';
import { Podcast } from '#app/shared/entity';

const PODCAST: Podcast = {
  id: '1f64b3a3-aaba-4282-a709-073299e3ef52',
  title: "Cauet S'Lâche",
  url: null,
  type: 'upload',
  lastUpdate: '2018-01-19T05:41:32.547+01:00',
  cover: {
    id: '6864693b-c37c-417c-8736-264e4fde095e',
    url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
    width: 400,
    height: 400
  },
  description: null,
  hasToBeDeleted: false,
  tags: [{ id: '5594cbbb-b126-476d-ae5b-72fc95d742bb', name: 'Cauet' }, { id: '7c3c823f-3c48-4ea5-ac6b-812d531bd4c9', name: 'Humour' }]
};


describe('PodcastResolver', () => {
	let resolver: PodcastResolver;
	let store;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [StoreModule.forRoot([]), StoreModule.forFeature('podcast', reducer)],
			providers: [PodcastResolver]
		});

		resolver = TestBed.get(PodcastResolver);
	});

	beforeEach(() => {
		store = TestBed.get(Store);
		spyOn(store, 'dispatch').and.callThrough();
	});

	it(
		'should be created',
		inject([PodcastResolver], (service: PodcastResolver) => {
			expect(service).toBeTruthy();
		})
	);

	it('should trigger findOne', () => {
		/* Given */
		const route = new ActivatedRouteSnapshot();
		route.params = { id: '88561083-9dbd-45a9-92c1-6ca5730e7f7c' };
		/* When  */
		resolver.resolve(route, null).subscribe(() => {});
		/* Then  */
		expect(store.dispatch).toHaveBeenCalledWith(new FindOneAction(route.params.id));
	});

	it(
		'should ignore the store state and get the new value from service',
		async(() => {
			/* Given */
			const route = new ActivatedRouteSnapshot();
			route.params = { id: '88561083-9dbd-45a9-92c1-6ca5730e7f7c' };

			/* When  */
			resolver.resolve(route, null).subscribe(v => {
				/* Then  */
				expect(v).toBe(PODCAST);
			});

			store.dispatch(new FindOneSuccessAction(PODCAST));
		})
	);
});
