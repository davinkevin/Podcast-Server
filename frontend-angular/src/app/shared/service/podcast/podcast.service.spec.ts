import {inject, TestBed} from '@angular/core/testing';

import {PodcastService} from './podcast.service';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';

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

  it('should be created', inject([PodcastService], (service: PodcastService) => {
    expect(service).toBeTruthy();
  }));

  it('should call for findAll', () => {
    /* Given */
    const resp = [];

    /* When  */
    service.findAll().subscribe((podcasts) => {
      expect(podcasts).toBe(resp);
    });

    /* Then  */
    const req = httpMock.expectOne('/api/podcasts');
    expect(req.request.method).toEqual('GET');
    req.flush(resp);
  });

  afterEach(inject([HttpTestingController], (httpMock: HttpTestingController) => {
    httpMock.verify();
  }));

});
