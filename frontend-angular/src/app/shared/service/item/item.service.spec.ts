/* tslint:disable:no-unused-variable */

import { TestBed } from '@angular/core/testing';

import { defaultSearch, ItemService } from './item.service';
import { Direction, Item, Page, Status } from 'app/shared/entity';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpParams } from '@angular/common/http';

const ITEM: Item = {
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
  length: 185263341,
  fileName: "Cauet sur Virgin - 2018-01-18 - A pas d'invité.mp3",
  status: 'FINISH',
  progression: 0,
  downloadDate: '2018-01-19T05:41:32.545+01:00',
  creationDate: '2018-01-19T05:41:32.547+01:00',
  proxyURL: '/api/podcasts/1f64b3a3-aaba-4282-a709-073299e3ef52/items/a725174f-6e61-4132-9c4f-795a9d2fc598/A_pas_d_invite_.mp3',
  isDownloaded: true,
  podcastId: '1f64b3a3-aaba-4282-a709-073299e3ef52'
};

describe('Service: Item', () => {
	const rootUrl = '/api/items';
	let service: ItemService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [ItemService],
			imports: [HttpClientTestingModule]
		});
	});

	beforeEach(() => {
		httpMock = TestBed.get(HttpTestingController);
		service = TestBed.get(ItemService);
	});

	it('should be defined', () => {
		expect(service).toBeTruthy();
	});

	it('should get all elements from backend', () => {
		/* Given */
		const expectedParams = new HttpParams()
			.set('page', '3')
			.set('size', '15')
			.set('sort', 'donwloadDate,DESC')
			.set('q', 'foo')
			.set('tags', 'tag')
			.set('status', 'STOPPED,FINISH');
		const body: Page<Item> = {
			content: [],
			first: true,
			last: true,
			totalPages: 10,
			totalElements: 100,
			numberOfElements: 10,
			size: 10,
			number: 3,
			sort: []
		};

		/* When */
		service
			.search({
				q: 'foo',
				page: 3,
				size: 15,
				status: [Status.STOPPED, Status.FINISH],
				tags: [{ name: 'tag', id: '1234' }],
				sort: [{ property: 'donwloadDate', direction: Direction.DESC }]
			})
			.subscribe(v => {
				expect(v).toEqual(body);
			});

		/* Then */
		const req = httpMock.expectOne(r => r.url === '/api/items/search');
		expect(req.request.method).toEqual('GET');
		expect(req.request.params.toString()).toEqual(expectedParams.toString());
		req.flush(body);
	});

	it('should support query without status', () => {
		/* Given */
		const expectedParams = new HttpParams()
			.set('page', '0')
			.set('size', '12')
			.set('sort', 'pubDate,DESC')
			.set('q', '')
			.set('tags', '');

		const body: Page<Item> = {
			content: [],
			first: true,
			last: false,
			totalPages: 10,
			totalElements: 100,
			numberOfElements: 10,
			size: 10,
			number: 3,
			sort: [{ direction: Direction.ASC, property: 'relevance' }]
		};

		/* When */
		service.search(defaultSearch).subscribe(v => {
			expect(v).toEqual(body);
		});

		/* Then */
		const req = httpMock.expectOne(r => r.url === '/api/items/search');
		expect(req.request.method).toEqual('GET');
		expect(req.request.params.toString()).toEqual(expectedParams.toString());
		req.flush(body);
	});

	it('should find by podcast and page', () => {
		/* Given */
		const id = 'b6f02232-37b6-4e86-a63a-319d89423d2b';
		const page = {
			page: 10,
			size: 5,
			sort: [{ direction: Direction.ASC, property: 'relevance' }]
		};
		const expectedParams = new HttpParams()
			.set('page', '10')
			.set('size', '5')
			.set('sort', 'relevance,ASC');

		const body: Page<Item> = {
			content: [],
			first: true,
			last: false,
			totalPages: 10,
			totalElements: 100,
			numberOfElements: 10,
			size: 10,
			number: 3,
			sort: [{ direction: Direction.ASC, property: 'relevance' }]
		};

		/* When */
		service.findByPodcastAndPage(id, page).subscribe(v => {
			expect(v).toEqual(body);
		});

		/* Then */
		const req = httpMock.expectOne(r => r.url === `/api/podcasts/${id}/items`);
		expect(req.request.method).toEqual('GET');
		expect(req.request.params.toString()).toEqual(expectedParams.toString());
		req.flush(body);
	});

	it('should find item by podcast-id and item-id', () => {
		/* Given */
		const itemId = 'b6f02232-37b6-4e86-a63a-319d89423d2b';
		const podcastId = 'ae84837a-68a4-4804-84d8-25a8347b94f7';
		const body =
			/* When */
			service.findById(itemId, podcastId).subscribe(v => {
				expect(v).toEqual(ITEM);
			});

		/* Then */
		const req = httpMock.expectOne(r => r.url === `/api/podcasts/${podcastId}/items/${itemId}`);
		expect(req.request.method).toEqual('GET');
		req.flush(ITEM);
	});

	afterEach(() => {
		httpMock.verify();
	});
});
