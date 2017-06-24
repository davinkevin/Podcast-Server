import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {SearchComponent} from './search.component';
import {SharedModule} from '../shared/shared.module';
import {RouterModule, Routes} from '@angular/router';

const routes: Routes = [
  { path: 'search', component: SearchComponent }
];


@NgModule({
  imports: [
    CommonModule, SharedModule,
    RouterModule.forChild(routes)
  ],
  exports: [SearchComponent],
  declarations: [SearchComponent]
})
export class SearchModule {}
