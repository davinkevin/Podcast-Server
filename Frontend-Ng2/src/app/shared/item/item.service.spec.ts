/* tslint:disable:no-unused-variable */

import { TestBed, async, inject } from '@angular/core/testing';
import { ItemService } from './item.service';
import {Http} from "@angular/http";

describe('Service: Item', () => {

  let itemService: ItemService, http: Http;

  beforeEach(() => {
    http = jasmine.createSpyObj('http', ['post']);
    itemService = new ItemService(http);
  });

  xit('should be defined', inject([ItemService], (service: ItemService) => {
    expect(service).toBeTruthy();
  }));

  it('should be defined', () => {
    expect(itemService).toBeDefined();
  });
});
