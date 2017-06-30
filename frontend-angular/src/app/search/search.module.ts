import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {SearchComponent} from './search.component';
import {SharedModule} from '../shared/shared.module';
import {RouterModule, Routes} from '@angular/router';
import {SearchResolver} from './resolver/search.resolver';

const routes: Routes = [
  { path: 'search',
    component: SearchComponent,
    resolve: { search: SearchResolver }
  }
];

@NgModule({
  imports: [
    CommonModule, SharedModule,
    RouterModule.forChild(routes)
  ],
  providers: [SearchResolver],
  exports: [SearchComponent],
  declarations: [SearchComponent]
})
export class SearchModule {}
