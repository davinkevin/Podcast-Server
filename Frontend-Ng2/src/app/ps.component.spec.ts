/* tslint:disable:no-unused-variable */

import { TestBed, async } from '@angular/core/testing';
import { PsComponent } from './ps.component';
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {ItemService} from "./shared/item/item.service";
import {HttpModule} from "@angular/http";

describe('App: Podcast-Server', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PsComponent],
      providers:[ItemService],
      imports:[HttpModule],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should create the app', async(() => {
    let fixture = TestBed.createComponent(PsComponent);
    let app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

  xit(`should have as title 'app works!'`, async(() => {
    let fixture = TestBed.createComponent(PsComponent);
    let app: PsComponent = fixture.debugElement.componentInstance;
    expect(app.itemsPage).toBeDefined();
  }));

  xit('should render title in a h1 tag', async(() => {
    let fixture = TestBed.createComponent(PsComponent);
    fixture.detectChanges();
    let compiled = fixture.debugElement.nativeElement;
    expect(compiled.querySelector('h1').textContent).toContain('app works!');
  }));
});
