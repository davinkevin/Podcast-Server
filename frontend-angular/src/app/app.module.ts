import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import 'hammerjs';

import {AppComponent} from './app.component';
import {MatIconModule, MatListModule, MatSidenavModule, MatToolbarModule} from '@angular/material';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SearchModule} from './search/search.module';
import {RouterModule, Routes} from '@angular/router';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastsModule} from './podcasts/podcasts.module';
import {environment} from '../environments/environment';
import {StoreDevtoolsModule} from '@ngrx/store-devtools';
import {sidenav} from './app.reducer';


const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full'}
];

const devModules = environment.production
  ? []
  : [ StoreDevtoolsModule.instrument({maxAge: 25}) ];


@NgModule({
  declarations: [AppComponent],
  imports: [
    /* std Modules */       BrowserModule, BrowserAnimationsModule,
    /* Materials Modules */ MatToolbarModule, MatIconModule, MatSidenavModule, MatListModule,
    /* Router Modules */    RouterModule.forRoot(routes),
    /* Feature Modules */   SearchModule, PodcastsModule,
    /* @ngrx */
    StoreModule.forRoot({sidenav}),
    EffectsModule.forRoot([]),
    /* Dev Modules */
    ...devModules
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
