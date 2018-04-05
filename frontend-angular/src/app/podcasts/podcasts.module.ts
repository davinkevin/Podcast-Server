import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatIconModule, MatToolbarModule } from '@angular/material';
import { RouterModule, Routes } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

import { SharedModule } from '../shared/shared.module';

import { PodcastsResolver } from './core/resolver/podcasts.resolver';
import { PodcastsComponent } from './podcasts.component';
import { PodcastsEffects } from './podcasts.effects';
import * as fromPodcasts from './podcasts.reducer';

const routes: Routes = [{ path: 'podcasts', component: PodcastsComponent, resolve: { podcasts: PodcastsResolver } }];

@NgModule({
	imports: [
		CommonModule,
		SharedModule,

		/* Material Design */
		MatToolbarModule,
		MatIconModule,

		/* Routes */
		RouterModule.forChild(routes),

		/* NgRx */
		StoreModule.forFeature('podcasts', fromPodcasts.reducer),
		EffectsModule.forFeature([PodcastsEffects])
	],
	providers: [PodcastsResolver],
	declarations: [PodcastsComponent]
})
export class PodcastsModule {}
