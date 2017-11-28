import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PodcastsComponent } from './podcasts.component';
import {RouterModule, Routes} from '@angular/router';
import {SharedModule} from '../shared/shared.module';
import {StoreModule} from '@ngrx/store';
import * as fromPodcasts from './podcasts.reducer';
import {EffectsModule} from '@ngrx/effects';
import {PodcastsEffects} from './podcasts.effects';
import {PodcastsResolver} from './resolver/podcasts.resolver';
import {MdCardModule} from '@angular/material';


const routes: Routes = [
  {
    path: 'podcasts', component: PodcastsComponent,
    resolve: {
      podcasts: PodcastsResolver
    }
  }
];

@NgModule({
  imports: [
    CommonModule, SharedModule,

    /* Material Design */
    MdCardModule,

    /* Routes */
    RouterModule.forChild(routes),

    /* NgRx */
    StoreModule.forFeature('podcastsModule', {
      podcasts: fromPodcasts.reducer
    }),
    EffectsModule.forFeature([PodcastsEffects])
  ],
  providers: [PodcastsResolver],
  declarations: [PodcastsComponent]
})
export class PodcastsModule { }
