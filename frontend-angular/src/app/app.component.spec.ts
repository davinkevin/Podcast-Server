import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AppComponent} from './app.component';
import {MdIconModule, MdToolbarModule} from '@angular/material';
import {RouterTestingModule} from '@angular/router/testing';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

describe('AppComponent', () => {

  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let el: DebugElement;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
      imports: [
        BrowserAnimationsModule,
        MdToolbarModule, MdIconModule,
        RouterTestingModule.withRoutes([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    el = fixture.debugElement;
    component = el.componentInstance;

    fixture.detectChanges();
  }));

  it('should create the app', async(() => {
    expect(component).toBeTruthy();
  }));

  it('should have 5 md-icons for each section in md-toolbar', () => {
    const icons = el.queryAll(By.css('md-toolbar md-icon'));
    expect(icons.length).toEqual(5);
  });

  it('should have router-outlet at root of component', () => {
    /* Given */
    const routerOutlet = el.query(By.css('router-outlet'));
    /* When  */

    /* Then  */
    expect(routerOutlet.parent.componentInstance).toBe(component);
  });
});
