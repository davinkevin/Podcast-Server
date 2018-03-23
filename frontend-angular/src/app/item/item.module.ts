import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ItemComponent} from './item.component';
import {RouterModule, Routes} from '@angular/router';
import {ItemResolver} from './core/item.resolver';
import {StoreModule} from '@ngrx/store';
import {itemReducer} from './item.reducer';
import {EffectsModule} from '@ngrx/effects';
import {ItemEffects} from './item.effects';
import {MatIconModule, MatToolbarModule} from '@angular/material';
import {SharedModule} from '../shared/shared.module';

const routes: Routes = [
  {
    path: 'podcasts/:podcastId/items/:id', component: ItemComponent,
    resolve: {item: ItemResolver}
  }
];


@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    MatIconModule,
    SharedModule,

    StoreModule.forFeature('item', itemReducer),
    EffectsModule.forFeature([ItemEffects])
  ],
  providers: [ItemResolver],
  declarations: [ItemComponent]
})
export class ItemModule { }
