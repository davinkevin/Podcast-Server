/* tslint:disable:no-unused-variable */

import 'rxjs/add/observable/of';

import {inject, TestBed} from '@angular/core/testing';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {ItemService} from './item.service';

xdescribe('Service: Item', () => {

  const http = jasmine.createSpyObj('http', ['post']);

  beforeEach(
      () => TestBed.configureTestingModule(
          {providers: [{provide: Http, useValue: http}, ItemService]}));

  it('should be defined',
     inject([ItemService], (service: ItemService) => { expect(service).toBeTruthy(); }));

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
