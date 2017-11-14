import {NgModule} from '@angular/core';
import {ItemService} from './service/item/item.service';
import {HttpModule} from '@angular/http';

@NgModule({
  imports: [HttpModule],
  providers: [ItemService]
})
export class SharedModule { }
