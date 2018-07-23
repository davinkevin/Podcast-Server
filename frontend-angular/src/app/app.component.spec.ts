import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AppComponent} from './app.component';
import {MatIconModule, MatListModule, MatSidenavModule, MatToolbarModule} from '@angular/material';
import {RouterTestingModule} from '@angular/router/testing';
import {DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {NoopAnimationsModule} from '@angular/platform-browser/animations';
import {Store, StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {AppState, sidenav} from './app.reducer';
import {CloseSideNavAction, OpenSideNavAction} from './app.actions';
import { FloatingPlayerModule } from '#app/floating-player/floating-player.module';
import { floatingPlayer } from '#app/floating-player/floating-player.reducer';

describe('AppComponent', () => {

  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let el: DebugElement;
  let store: Store<AppState>;


  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [
        AppComponent
      ],
      imports: [
        NoopAnimationsModule,
        MatToolbarModule, MatIconModule, MatSidenavModule, MatListModule,
        RouterTestingModule.withRoutes([]),
        StoreModule.forRoot({sidenav, floatingPlayer}),
        EffectsModule.forRoot([]),
        FloatingPlayerModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    el = fixture.debugElement;
    component = el.componentInstance;

    fixture.detectChanges();
  }));

  beforeEach(() => {
    store = TestBed.get(Store);
    spyOn(store, 'dispatch').and.callThrough();
    spyOn(store, 'select').and.callThrough();
  });

  it('should create the app', async(() => {
    expect(component).toBeTruthy();
  }));

  it('should have 4 elements in side nav', async(() => {
    /* Given */
    /* When  */
    const sideEls = el.queryAll(By.css('[mat-list-item]'));
    /* Then  */
    expect(sideEls.length).toEqual(4);
  }));

  it('should have sidenav closed at boot', () => {
    /* Given */
    const sidenavComp = el.query(By.css('mat-sidenav'));
    /* When  */
    const opened = sidenavComp.componentInstance.opened;
    /* Then  */
    expect(opened).toBeFalsy();
  });

  describe('with the sidenav open', () => {
    let sidenavComp: DebugElement;

    beforeEach(async () => {
      sidenavComp = el.query(By.css('mat-sidenav'));
      /* When  */
      store.dispatch(new OpenSideNavAction());
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('should open the sidenav if store tell it to', async () => {
      expect(sidenavComp.componentInstance.opened).toBeTruthy();
    });

    it('should propagate state of the sidenav if something closed it', async () => {
      /* Given */
      /* When  */
      store.dispatch(new CloseSideNavAction());
      fixture.detectChanges();
      await fixture.whenStable();
      /* Then  */
      expect(store.dispatch).toHaveBeenCalledWith(new CloseSideNavAction());
    });
  });


});
