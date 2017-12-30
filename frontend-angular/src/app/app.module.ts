import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import 'hammerjs';

import {AppComponent} from './app.component';
import {MatIconModule, MatToolbarModule} from '@angular/material';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {SearchModule} from './search/search.module';
import {RouterModule, Routes} from '@angular/router';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastsModule} from './podcasts/podcasts.module';

const routes: Routes = [
  { path: '', redirectTo: '/search', pathMatch: 'full'}
];

@NgModule({
  declarations: [AppComponent],
  imports: [
    /* std Modules */       BrowserModule, BrowserAnimationsModule,
    /* Materials Modules */ MatToolbarModule, MatIconModule,
    /* Router Modules */    RouterModule.forRoot(routes),
    /* Feature Modules */   SearchModule, PodcastsModule,
    /* @ngrx */             StoreModule.forRoot({}), EffectsModule.forRoot([])
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
