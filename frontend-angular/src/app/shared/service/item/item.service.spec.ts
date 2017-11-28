/* tslint:disable:no-unused-variable */

import {inject, TestBed} from '@angular/core/testing';

import {defaultSearch, ItemService} from './item.service';
import {Direction, Item, Page, Status} from 'app/shared/entity';
import {HttpClientTestingModule, HttpTestingController} from '@angular/common/http/testing';
import {HttpParams} from '@angular/common/http';

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

  it('should be defined', inject([ItemService], (service: ItemService) => {
    expect(service).toBeTruthy();
  }));

  it('should get all elements from backend', () => {
    /* Given */
    const expectedParams = new HttpParams()
      .set('q', 'foo')
      .set('page', '3')
      .set('size','15')
      .set('sort', 'donwloadDate,DESC')
      .set('tags', 'tag')
      .set('status', 'STOPPED,FINISH')
    ;
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
    service.search({q: 'foo', page: 3, size: 15,
        status:[Status.STOPPED, Status.FINISH], tags:[{name:'tag', id:'1234'}],
        sort: [{property: 'donwloadDate', direction: Direction.DESC}]
      }).subscribe(v => {
      expect(v).toEqual(body);
    });

    /* Then */
    const req = httpMock.expectOne(req => req.url === '/api/items/search');
    expect(req.request.method).toEqual('GET');
    expect(req.request.params.toString()).toEqual(expectedParams.toString());
    req.flush(body);
  });

  it('should support query without status', () => {
    /* Given */
    const expectedParams = new HttpParams()
      .set('q', '')
      .set('page', '0')
      .set('size','12')
      .set('sort', 'pubDate,DESC')
      .set('tags', '')
    ;

    const body: Page<Item> = {
      content: [],
      first: true,
      last: false,
      totalPages: 10,
      totalElements: 100,
      numberOfElements: 10,
      size: 10,
      number: 3,
      sort: [{direction: Direction.ASC, property: 'relevance'}]
    };

    /* When */
    service.search(defaultSearch).subscribe(v => {
      expect(v).toEqual(body);
    });

    /* Then */
    const req = httpMock.expectOne(req => req.url === '/api/items/search');
    expect(req.request.method).toEqual('GET');
    expect(req.request.params.toString()).toEqual(expectedParams.toString());
    req.flush(body);
  });

  afterEach(inject([HttpTestingController], (httpMock: HttpTestingController) => {
    httpMock.verify();
  }));
});
