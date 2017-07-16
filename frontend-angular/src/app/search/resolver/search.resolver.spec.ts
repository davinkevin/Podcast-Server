import { TestBed, inject } from '@angular/core/testing';

import { SearchResolver } from './search.resolver';

xdescribe('SearchResolver', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SearchResolver]
    });
  });

  it('should ...', inject([SearchResolver], (service: SearchResolver) => {
    expect(service).toBeTruthy();
  }));
});
