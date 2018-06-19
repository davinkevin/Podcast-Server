import { Store, StoreModule } from '@ngrx/store';
import { reducer } from '../podcast.reducer';
import { async, inject, TestBed } from '@angular/core/testing';
import { PodcastItemsResolver, toPodcastPageOfItems } from './podcast-items.resolver';
import { FindItemsByPodcastsAndPageAction, FindItemsByPodcastsAndPageSuccessAction } from '../podcast.actions';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Direction, Item, Page, Pageable } from '#app/shared/entity';

const PAGE_ITEMS: Page<Item> = {
  content: [
    {
      id: 'a725174f-6e61-4132-9c4f-795a9d2fc598',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: "A pas d'invité",
      url: null,
      pubDate: '2018-01-18T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-19T05:41:32.547+01:00',
      proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/a725174f-6e61-4132-9c4f-795a9d2fc598/A_pas_d_invite_.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: 'da0f69f5-235e-4b1b-8719-8fadc1e1cf85',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: "A pas d'invité",
      url: null,
      pubDate: '2018-01-17T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-18T05:51:17.482+01:00',
      proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/da0f69f5-235e-4b1b-8719-8fadc1e1cf85/A_pas_d_invite_.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: '76f4c282-9545-42a3-aeaa-d6826fba0a84',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title:
        'Pierre Lottin, Alex Ramirès, Margot Bancilhon, Kheiron, Claudia Tagbo pour de vrai, Arnaud Tsamere, Synapson, Feder, Jonathan Cohen et la Compagnie Créole',
      url: null,
      pubDate: '2018-01-16T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-17T05:53:01.716+01:00',
      proxyURL:
        '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/76f4c282-9545-42a3-aeaa-d6826fba0a84/Pierre_Lottin__Alex_Ramire_s__Margot_Bancilhon__Kheiron__Claudia_Tagbo_pour_de_vrai__Arnaud_Tsamere__Synapson__Feder__Jonathan_Cohen_et_la_Compagnie_Cre_ole.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: 'ebd64473-f5da-4759-8697-870a0538b005',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: "A pas d'invité",
      url: null,
      pubDate: '2018-01-15T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-16T05:57:04.04+01:00',
      proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/ebd64473-f5da-4759-8697-870a0538b005/A_pas_d_invite_.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: '56eeca14-364b-4f58-8f5b-e5d573046840',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: 'Petit Karaoké de la montagne',
      url: null,
      pubDate: '2018-01-12T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-13T08:06:19.786+01:00',
      proxyURL:
        '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/56eeca14-364b-4f58-8f5b-e5d573046840/Petit_Karaoke__de_la_montagne.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: '928a8556-9442-4c4b-a9e2-3d0ed4ff059f',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: 'En direct de la montagne',
      url: null,
      pubDate: '2018-01-11T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-12T05:50:15.679+01:00',
      proxyURL:
        '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/928a8556-9442-4c4b-a9e2-3d0ed4ff059f/En_direct_de_la_montagne.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: 'a550522c-5e21-47cf-a370-cdbc3fd30694',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: 'Premier jour à la montagne',
      url: null,
      pubDate: '2018-01-10T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-11T06:00:27.157+01:00',
      proxyURL:
        '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/a550522c-5e21-47cf-a370-cdbc3fd30694/Premier_jour_a__la_montagne.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: 'bfd24522-b856-4d3b-a912-e2278f4bddb5',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: 'William Lebghil, Nâdiya, Julien Lepers, Claudia Tagbo, Antonia de Rendinger, Issa Dombia et Victor Saint Macary',
      url: null,
      pubDate: '2018-01-09T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-10T05:51:13.915+01:00',
      proxyURL:
        '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/bfd24522-b856-4d3b-a912-e2278f4bddb5/William_Lebghil__Na_diya__Julien_Lepers__Claudia_Tagbo__Antonia_de_Rendinger__Issa_Dombia_et_Victor_Saint_Macary.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: 'c41fc0b4-7cfa-4657-80c5-bc147c74af1a',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: "A pas d'invité",
      url: null,
      pubDate: '2018-01-08T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-09T06:00:19.049+01:00',
      proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/c41fc0b4-7cfa-4657-80c5-bc147c74af1a/A_pas_d_invite_.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    },
    {
      id: '6192f1c1-fb77-4de5-929b-e4b0fd5b8d67',
      cover: {
        id: '6864693b-c37c-417c-8736-264e4fde095e',
        url: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/cover.jpg',
        width: 400,
        height: 400
      },
      title: 'Soirée du 31',
      url: null,
      pubDate: '2018-01-05T00:00:00+01:00',
      description: null,
      mimeType: 'audio/mp3',
      status: 'FINISH',
      creationDate: '2018-01-06T16:44:30.781+01:00',
      proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/6192f1c1-fb77-4de5-929b-e4b0fd5b8d67/Soire_e_du_31.mp3',
      isDownloaded: true,
      podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
    }
  ],
  totalPages: 10,
  totalElements: 96,
  last: false,
  numberOfElements: 10,
  first: true,
  sort: [{ direction: 'DESC', property: 'pubDate', ignoreCase: false, nullHandling: 'NATIVE', descending: true, ascending: false }],
  size: 10,
  number: 0
};


describe('PodcastItemsResolver', () => {
	let resolver: PodcastItemsResolver;
	let store;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [StoreModule.forRoot([]), StoreModule.forFeature('podcast', reducer)],
			providers: [PodcastItemsResolver]
		});

		resolver = TestBed.get(PodcastItemsResolver);
	});

	beforeEach(() => {
		store = TestBed.get(Store);
		spyOn(store, 'dispatch').and.callThrough();
	});

	it(
		'should be created',
		inject([PodcastItemsResolver], (service: PodcastItemsResolver) => {
			expect(service).toBeTruthy();
		})
	);

	it('should trigger findOne', () => {
		/* Given */
		const route = new ActivatedRouteSnapshot();
		route.params = { id: '88561083-9dbd-45a9-92c1-6ca5730e7f7c' };
		const defaultPage: Pageable = { page: 0, size: 10, sort: [{ property: 'pubDate', direction: Direction.DESC }] };
		/* When  */
		resolver.resolve(route, null).subscribe(() => {});
		/* Then  */
		expect(store.dispatch).toHaveBeenCalledWith(new FindItemsByPodcastsAndPageAction(route.params.id, defaultPage));
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
				expect(v).toBe(PAGE_ITEMS);
			});

			store.dispatch(new FindItemsByPodcastsAndPageSuccessAction(PAGE_ITEMS));
		})
	);
});
