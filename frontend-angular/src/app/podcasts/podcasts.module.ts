import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PodcastsComponent} from './podcasts.component';
import {RouterModule, Routes} from '@angular/router';
import {SharedModule} from '../shared/shared.module';
import {StoreModule} from '@ngrx/store';
import * as fromPodcasts from './podcasts.reducer';
import {EffectsModule} from '@ngrx/effects';
import {PodcastsEffects} from './podcasts.effects';
import {PodcastsResolver} from './core/resolver/podcasts.resolver';
import {MatIconModule, MatToolbarModule} from '@angular/material';
import {PodcastModule} from './podcast/podcast.module';
import {PodcastService} from './shared/service/podcast/podcast.service';

const routes: Routes = [
  {
    path: 'podcasts', component: PodcastsComponent,
    resolve: {podcasts: PodcastsResolver}
  }
];

@NgModule({
  imports: [
    CommonModule, SharedModule,

    /* Material Design */
    MatToolbarModule, MatIconModule,

    /* Routes */
    RouterModule.forChild(routes),

    /* Features */
    PodcastModule,

    /* NgRx */
    StoreModule.forFeature('podcasts', fromPodcasts.reducer),
    EffectsModule.forFeature([PodcastsEffects])
  ],
  providers: [PodcastsResolver, PodcastService],
  declarations: [PodcastsComponent]
})
export class PodcastsModule { }
