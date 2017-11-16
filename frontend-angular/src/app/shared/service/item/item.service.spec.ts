/* tslint:disable:no-unused-variable */

import {inject, TestBed} from '@angular/core/testing';
import {BaseRequestOptions, Http, HttpModule, RequestMethod, Response, ResponseOptions, XHRBackend} from '@angular/http';

import {ItemService} from './item.service';
import {MockBackend, MockConnection} from '@angular/http/testing';
import {Direction, Item, Page, Status} from 'app/shared/entity';

describe('Service: Item', () => {

  const rootUrl = '/api/items';
  let mockBackend: MockBackend;
  let itemService: ItemService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ItemService, MockBackend, BaseRequestOptions,
        {
          provide: Http, deps: [MockBackend, BaseRequestOptions],
          useFactory: (b: XHRBackend, o: BaseRequestOptions) => new Http(b, o)
        }
      ],
      imports: [HttpModule]
    });
    mockBackend = TestBed.get(MockBackend);
    itemService = TestBed.get(ItemService);
  });

  it('should be defined', inject([ItemService], (service: ItemService) => {
    expect(service).toBeTruthy();
  }));

  it('should get all elements from backend with default parameter', () => {
    /* Given */
    const body: Page<Item> = {
      content:[],
      first:true,
      last:true,
      totalPages:10,
      totalElements: 100,
      numberOfElements: 10,
      size: 10,
      number: 3,
      sort: []
    };
    let conn: MockConnection;

    mockBackend.connections.subscribe((c: MockConnection) => {
      c.mockRespond(new Response(new ResponseOptions({body})));
      conn = c;
    });

    /* When */
    itemService.search(ItemService.defaultSearch).subscribe(v => {
      expect(v).toEqual(body);
    });

    /* Then */
    expect(conn.request.method).toEqual(RequestMethod.Get);
    expect(conn.request.url).toContain(rootUrl + '/search');
    expect(conn.request.url).toContain('page=0');
    expect(conn.request.url).toContain('size=12');
    expect(conn.request.url).toContain('sort=pubDate,DESC');
    expect(conn.request.url).toContain('tags=');
  });

  it('should get all elements from backend with default parameter', () => {
    /* Given */
    const body: Page<Item> = {
      content:[],
      first:true,
      last:false,
      totalPages:10,
      totalElements: 100,
      numberOfElements: 10,
      size: 10,
      number: 3,
      sort: [{direction: Direction.ASC, property:"relevance"}]
    };
    let conn: MockConnection;

    mockBackend.connections.subscribe((c: MockConnection) => {
      c.mockRespond(new Response(new ResponseOptions({body})));
      conn = c;
    });

    /* When */
    itemService.search({
      page: 3,
      size: 10,
      status: [Status.STARTED, Status.FINISH],
      sort: [{direction: Direction.ASC, property: 'foo'}],
      tags: [{id: 'id', name: 'bar'}]
    }).subscribe(v => {
      expect(v).toEqual(body);
    });

    /* Then */
    expect(conn.request.method).toEqual(RequestMethod.Get);
    expect(conn.request.url).toContain(rootUrl + '/search');
    expect(conn.request.url).toContain('page=3');
    expect(conn.request.url).toContain('size=10');
    expect(conn.request.url).toContain('status');
    expect(conn.request.url).toContain('sort=foo,ASC');
    expect(conn.request.url).toContain('tags=bar');
  });

});
