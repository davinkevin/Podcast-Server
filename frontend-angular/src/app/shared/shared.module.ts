import {NgModule} from '@angular/core';
import {ItemService} from './service/item/item.service';
import {HttpModule} from '@angular/http';
import {HttpClientModule} from '@angular/common/http';
import {PodcastService} from './service/podcast/podcast.service';
import {ToolbarModule} from './toolbar/toolbar.module';

@NgModule({
  imports: [HttpModule, HttpClientModule, ToolbarModule],
  exports: [ToolbarModule],
  providers: [ItemService, PodcastService]
})
export class SharedModule { }
