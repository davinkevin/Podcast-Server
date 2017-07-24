import { TestBed, inject } from '@angular/core/testing';

import { SearchResolver } from './search.resolver';
import {ItemService} from '../../shared/service/item/item.service';
import {ActivatedRouteSnapshot} from '@angular/router';
import Spy = jasmine.Spy;
import {Observable} from 'rxjs/Observable';

describe('SearchResolver', () => {

  let itemService: ItemService;
  let route: ActivatedRouteSnapshot;

  beforeEach(() => {
    itemService = jasmine.createSpyObj('itemService', ['search']);
    (itemService.search as Spy).and.returnValue(Observable.of({}));
  });

  beforeEach(() => {
    route = <ActivatedRouteSnapshot> {
      queryParamMap: jasmine.createSpyObj('queryParamMap', ['get'])
    };
    (route.queryParamMap.get as Spy).and.returnValue(3);
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SearchResolver,
        {provide: ItemService, useValue: itemService},
        {provide: ActivatedRouteSnapshot, useValue: route},
      ]
    });
  });

  it('should be able to inject service', inject([SearchResolver], (service: SearchResolver) => {
    expect(service).toBeTruthy();
  }));
});
