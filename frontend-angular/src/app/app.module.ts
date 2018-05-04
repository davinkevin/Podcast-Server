import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import 'hammerjs';

import { AppComponent } from './app.component';
import { MatIconModule, MatListModule, MatSidenavModule } from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { SearchModule } from './search/search.module';
import { RouterModule, Routes } from '@angular/router';
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { PodcastsModule } from './podcasts/podcasts.module';
import { devTools } from '../environments/environment';
import { sidenav } from './app.reducer';
import { PodcastModule } from './podcast/podcast.module';
import { ItemModule } from './item/item.module';
import { routerReducer as router, StoreRouterConnectingModule } from '@ngrx/router-store';
import { AppEffects } from './app.effects';
import { LocationStoreHelperModule, RouterStoreHelperModule } from '@davinkevin/router-store-helper';
import { FloatingPlayerModule } from '#app/floating-player/floating-player.module';
import { floatingPlayer } from '#app/floating-player/floating-player.reducer';

const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full'}
];

@NgModule({
  declarations: [AppComponent],
  imports: [
    /* std Modules */       BrowserModule, BrowserAnimationsModule,
    /* Materials Modules */ MatIconModule, MatSidenavModule, MatListModule,
    /* Router Modules */    RouterModule.forRoot(routes),
    /* Feature Modules */   SearchModule, PodcastsModule, PodcastModule, ItemModule, FloatingPlayerModule,
    /* @ngrx */
    StoreModule.forRoot({sidenav, router}),
    EffectsModule.forRoot([AppEffects]),
    StoreRouterConnectingModule.forRoot({stateKey: 'router'}),
    LocationStoreHelperModule, RouterStoreHelperModule,
    /* Dev Modules */
    ...devTools
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
