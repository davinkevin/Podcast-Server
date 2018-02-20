import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule, Routes} from '@angular/router';
import {PodcastComponent} from './podcast.component';
import {PodcastResolver} from './core/podcast.resolver';
import * as fromPodcast from './podcast.reducer';
import {StoreModule} from '@ngrx/store';
import {EffectsModule} from '@ngrx/effects';
import {PodcastEffects} from 'app/podcasts/podcast/podcast.effects';
import {MatButtonModule, MatIconModule, MatListModule, MatMenuModule, MatToolbarModule} from '@angular/material';
import {PodcastItemsResolver} from './core/podcast-items.resolver';
import {EpisodesComponent} from './core/episodes/episodes.component';


const routes: Routes = [
  {
    path: 'podcasts/:id', component: PodcastComponent,
    resolve: {podcast: PodcastResolver},
    children: [
      {
        path: '',
        component: EpisodesComponent,
        resolve: {
          items: PodcastItemsResolver,
        }
      }
    ]
  }
];

@NgModule({
  imports: [
    CommonModule,

    /* Material */
    MatIconModule, MatButtonModule, MatMenuModule, MatToolbarModule, MatListModule,

    /* Routes */
    RouterModule.forChild(routes),

    /* NgRx */
    StoreModule.forFeature('podcast', fromPodcast.reducer),
    EffectsModule.forFeature([PodcastEffects])
  ],
  providers: [PodcastResolver, PodcastItemsResolver],
  declarations: [PodcastComponent, EpisodesComponent],
  exports: [RouterModule]
})
export class PodcastModule {}
