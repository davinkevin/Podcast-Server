import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule, MatIconModule, MatListModule, MatMenuModule, MatPaginatorModule } from '@angular/material';
import { RouterModule, Routes } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

import { SharedModule } from '../shared/shared.module';

import { EpisodesComponent } from './core/episodes/episodes.component';
import { PodcastItemsResolver } from './core/podcast-items.resolver';
import { PodcastResolver } from './core/podcast.resolver';
import { PodcastComponent } from './podcast.component';
import { PodcastEffects } from './podcast.effects';
import * as fromPodcast from './podcast.reducer';

const routes: Routes = [
	{
		path: 'podcasts/:id',
		component: PodcastComponent,
		resolve: { podcast: PodcastResolver },
		children: [
			{
				path: '',
				component: EpisodesComponent,
				resolve: {
					items: PodcastItemsResolver
				}
			}
		]
	}
];

@NgModule({
	imports: [
		CommonModule,
		SharedModule,

		/* Material */
		MatIconModule,
		MatButtonModule,
		MatMenuModule,
		MatListModule,
		MatPaginatorModule,

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
