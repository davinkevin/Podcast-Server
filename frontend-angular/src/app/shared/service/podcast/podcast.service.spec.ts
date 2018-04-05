import { TestBed } from '@angular/core/testing';

import { PodcastService } from './podcast.service';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

describe('PodcastService', () => {
	let service: PodcastService;
	let httpMock: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [HttpClientTestingModule],
			providers: [PodcastService]
		});
	});

	beforeEach(() => {
		httpMock = TestBed.get(HttpTestingController);
		service = TestBed.get(PodcastService);
	});

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('should call for findAll', () => {
		/* Given */
		const resp = [];

		/* When  */
		service.findAll().subscribe(podcasts => {
			expect(podcasts).toBe(resp);
		});

		/* Then  */
		const req = httpMock.expectOne('/api/podcasts');
		expect(req.request.method).toEqual('GET');
		req.flush(resp);
	});

	it('should call for findOne', () => {
		/* Given */
		const resp = {
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
		};

		/* When  */
		service.findOne('8ba490ac-8f9a-4e2d-8758-b65e783e783a').subscribe(podcast => {
			expect(podcast).toBe(resp);
		});

		/* Then  */
		const req = httpMock.expectOne(`/api/podcasts/${resp.id}`);
		expect(req.request.method).toEqual('GET');
		req.flush(resp);
	});

	it('should call refresh', () => {
		/* Given */
		const podcast = {
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
		};

		/* When  */
		service.refresh(podcast).subscribe();

		/* Then  */
		const req = httpMock.expectOne(`/api/podcasts/${podcast.id}/update/force`);
		expect(req.request.method).toEqual('GET');
		req.flush(null);
	});

	afterEach(() => {
		httpMock.verify();
	});
});
