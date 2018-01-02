import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Routes} from '@angular/router';
import {PodcastComponent} from './podcast.component';
import {SharedModule} from '../../shared/shared.module';
import {PodcastResolver} from './core/resolver/podcast.resolver';
import * as fromPodcast from './podcast.reducer';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastEffects} from 'app/podcasts/podcast/podcast.effects';


const routes: Routes = [
  {
    path: 'podcasts/:id', component: PodcastComponent,
    resolve: {podcast: PodcastResolver}
  }
];

@NgModule({
  imports: [
    CommonModule, SharedModule,

    /* Routes */
    RouterModule.forChild(routes),

    /* NgRx */
    StoreModule.forFeature('podcast', fromPodcast.reducer),
    EffectsModule.forFeature([PodcastEffects])
  ],
  providers: [PodcastResolver, PodcastEffects],
  declarations: [PodcastComponent],
  exports: [RouterModule]
})
export class PodcastModule {}
