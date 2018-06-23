import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule, MatDividerModule, MatIconModule, MatListModule, MatMenuModule } from '@angular/material';
import { RouterModule, Routes } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

import { SharedModule } from '../shared/shared.module';

import { ItemResolver } from './core/item.resolver';
import { ItemComponent } from './item.component';
import { ItemEffects } from './item.effects';
import { itemReducer } from './item.reducer';
import { PodcastResolver } from '#app/item/core/podcast.resolver';

const routes: Routes = [
	{
		path: 'podcasts/:podcastId/items/:id',
		component: ItemComponent,
		resolve: {
			item: ItemResolver,
			podcast: PodcastResolver
		}
	}
];

@NgModule({
	imports: [
		CommonModule,
		RouterModule.forChild(routes),
		MatIconModule,
		MatDividerModule,
		MatListModule,
		MatButtonModule,
		MatMenuModule,
		SharedModule,

		StoreModule.forFeature('item', itemReducer),
		EffectsModule.forFeature([ItemEffects])
	],
	providers: [ItemResolver, PodcastResolver],
	declarations: [ItemComponent]
})
export class ItemModule {}
