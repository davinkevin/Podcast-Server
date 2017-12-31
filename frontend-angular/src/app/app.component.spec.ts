import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AppComponent} from './app.component';
import {MatIconModule, MatToolbarModule} from '@angular/material';
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
        MatIconModule, MatToolbarModule,
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

  it('should have 5 mat-icons for each section in mat-toolbar', () => {
    const icons = el.queryAll(By.css('mat-toolbar mat-icon'));
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
