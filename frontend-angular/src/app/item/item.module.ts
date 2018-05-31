import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule, MatDividerModule, MatIconModule, MatListModule } from '@angular/material';
import { RouterModule, Routes } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';

import { SharedModule } from '../shared/shared.module';

import { ItemResolver } from './core/item.resolver';
import { ItemComponent } from './item.component';
import { ItemEffects } from './item.effects';
import { itemReducer } from './item.reducer';

const routes: Routes = [{ path: 'podcasts/:podcastId/items/:id', component: ItemComponent, resolve: { item: ItemResolver } }];

@NgModule({
	imports: [
		CommonModule,
		RouterModule.forChild(routes),
		MatIconModule,
		MatDividerModule,
		MatListModule,
		MatButtonModule,
		SharedModule,

		StoreModule.forFeature('item', itemReducer),
		EffectsModule.forFeature([ItemEffects])
	],
	providers: [ItemResolver],
	declarations: [ItemComponent]
})
export class ItemModule {}
