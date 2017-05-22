/* tslint:disable:no-unused-variable */

import { TestBed, inject } from '@angular/core/testing';
import { ItemService } from './item.service';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';

describe('Service: Item', () => {

  const http = jasmine.createSpyObj('http', ['post']);

  beforeEach(() => TestBed.configureTestingModule({
    providers: [
      { provide: Http, useValue: http },
      ItemService
    ]
  }));

  it('should be defined', inject([ItemService], (service: ItemService) => {
    expect(service).toBeTruthy();
  }));

  xit('should get all elements from backend', inject([ItemService], (service: ItemService) => {
    /* Given */
    const pageItem = {};
    const httpResponse = jasmine.createSpyObj('response', ['json']);
    let response = null;
    httpResponse.json.and.returnValue(pageItem);
    http.post.and.returnValue(Observable.of(httpResponse));

    /* When */
    service.search().subscribe(v => response = v);

    /* Then */
    expect(response).toEqual(pageItem);
  }));

});
