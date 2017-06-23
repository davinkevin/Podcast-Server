import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {SearchComponent} from './search.component';
import {SharedModule} from '../shared/shared.module';

@NgModule({
  imports: [CommonModule, SharedModule],
  exports: [SearchComponent],
  declarations: [SearchComponent]
})
export class SearchModule { }
