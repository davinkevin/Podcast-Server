import {NgModule} from '@angular/core';
import {ItemService} from './service/item/item.service';
import {HttpModule} from '@angular/http';
import {HttpClientModule} from '@angular/common/http';

@NgModule({
  imports: [HttpModule, HttpClientModule],
  providers: [ItemService]
})
export class SharedModule { }
