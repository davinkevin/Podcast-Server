import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Routes} from '@angular/router';
import {PodcastComponent} from './podcast.component';
import {PodcastResolver} from './core/resolver/podcast.resolver';
import * as fromPodcast from './podcast.reducer';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastEffects} from 'app/podcasts/podcast/podcast.effects';
import {MatButtonModule, MatIconModule, MatMenuModule} from '@angular/material';


const routes: Routes = [
  {
    path: 'podcasts/:id', component: PodcastComponent,
    resolve: {podcast: PodcastResolver}
  }
];

@NgModule({
  imports: [
    CommonModule,

    /* Material */
    MatIconModule, MatButtonModule, MatMenuModule,

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
