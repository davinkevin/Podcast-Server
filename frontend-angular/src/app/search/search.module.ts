import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import {
	MatButtonModule,
	MatCardModule,
	MatIconModule,
	MatInputModule,
	MatPaginatorModule,
	MatSelectModule,
	MatToolbarModule
} from '@angular/material';
import { RouterModule, Routes } from '@angular/router';
import { EffectsModule } from '@ngrx/effects';
import { StoreModule } from '@ngrx/store';
import { SearchEffects } from 'app/search/search.effects';
import { TruncateModule } from 'ng2-truncate';

import { SharedModule } from '../shared/shared.module';

import { SearchQueryResolver } from './resolver/search-query.resolver';
import { SearchResolver } from './resolver/search.resolver';
import { SearchComponent } from './search.component';
import { search } from '#app/search/search.reducer';

const routes: Routes = [
	{
		path: 'search',
		component: SearchComponent,
		resolve: { search: SearchResolver, request: SearchQueryResolver }
	}
];

@NgModule({
	imports: [
		CommonModule,
		SharedModule,

		/* Forms */
		ReactiveFormsModule,

		/* Routes */
		RouterModule.forChild(routes),

		/* Material Design */
		MatCardModule,
		MatButtonModule,
		MatIconModule,
		MatInputModule,
		MatSelectModule,
		MatPaginatorModule,
		MatToolbarModule,

		/* 3rd party module */
		TruncateModule,

		/* NgRx */
		StoreModule.forFeature('search', search),
		EffectsModule.forFeature([SearchEffects])
	],
	providers: [SearchResolver, SearchQueryResolver],
	exports: [SearchComponent],
	declarations: [SearchComponent]
})
export class SearchModule {}
