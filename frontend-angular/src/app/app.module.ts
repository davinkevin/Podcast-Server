import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import 'hammerjs';

import {AppComponent} from './app.component';
import {MatIconModule, MatListModule, MatSidenavModule, MatToolbarModule} from '@angular/material';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SearchModule} from './search/search.module';
import {RouterModule, RouterStateSnapshot, Routes} from '@angular/router';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastsModule} from './podcasts/podcasts.module';
import {devTools} from '../environments/environment';
import {sidenav} from './app.reducer';
import {PodcastModule} from './podcast/podcast.module';
import {ItemModule} from './item/item.module';
import {RouterStateSerializer, StoreRouterConnectingModule, routerReducer as router} from '@ngrx/router-store';
import {AppEffects} from './app.effects';

const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full'}
];

export class CustomSerializer implements RouterStateSerializer<RouterStateUrl> {
  serialize(routerState: RouterStateSnapshot): RouterStateUrl {
    let route = routerState.root;

    while (route.firstChild) {
      route = route.firstChild;
    }

    const { url, root: { queryParams } } = routerState;
    const { params } = route;

    return { url, params, queryParams };
  }
}


@NgModule({
  declarations: [AppComponent],
  imports: [
    /* std Modules */       BrowserModule, BrowserAnimationsModule,
    /* Materials Modules */ MatIconModule, MatSidenavModule, MatListModule,
    /* Router Modules */    RouterModule.forRoot(routes),
    /* Feature Modules */   SearchModule, PodcastsModule, PodcastModule, ItemModule,
    /* @ngrx */
    StoreModule.forRoot({sidenav, router}),
    EffectsModule.forRoot([AppEffects]),
    StoreRouterConnectingModule.forRoot({stateKey: 'router'}),
    /* Dev Modules */
    ...devTools
  ],
  providers: [
    { provide: RouterStateSerializer, useClass: CustomSerializer }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
