import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SearchComponent} from './search.component';
import {SharedModule} from '../shared/shared.module';
import {RouterModule, Routes} from '@angular/router';
import {SearchResolver} from './resolver/search.resolver';
import {
  MatButtonModule, MatCardModule, MatIconModule, MatInputModule, MatOptionModule, MatPaginator, MatPaginatorModule,
  MatSelectModule
} from '@angular/material';
import {StoreModule} from '@ngrx/store';
import * as fromSearch from './search.reducer';
import {EffectsModule} from '@ngrx/effects';
import {SearchEffects} from 'app/search/search.effects';
import {ReactiveFormsModule} from '@angular/forms';
import {SearchQueryResolver} from './resolver/search-query.resolver';
import {TruncateModule} from 'ng2-truncate';


const routes: Routes = [
  { path: 'search',
    component: SearchComponent,
    resolve: {
      search: SearchResolver,
      request: SearchQueryResolver
    }
  }
];

@NgModule({
  imports: [
    CommonModule, SharedModule,

    /* Forms */
    ReactiveFormsModule,

    /* Routes */
    RouterModule.forChild(routes),

    /* Material Design */
    MatCardModule, MatButtonModule, MatIconModule, MatInputModule, MatSelectModule,
    MatPaginatorModule,

    /* 3rd party module */
    TruncateModule,

    /* NgRx */
    StoreModule.forFeature('searchModule', {
      search: fromSearch.reducer
    }),
    EffectsModule.forFeature([SearchEffects])
  ],
  providers: [SearchResolver, SearchQueryResolver],
  exports: [SearchComponent],
  declarations: [SearchComponent]
})
export class SearchModule {}
