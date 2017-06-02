import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { AppComponent } from './app.component';
import { ItemService } from './shared/service/item/item.service';
import { MdToolbarModule, MdIconModule } from '@angular/material';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

@NgModule({
  declarations: [AppComponent],
  imports: [BrowserModule, FormsModule, HttpModule,
    MdToolbarModule, MdIconModule,
    BrowserAnimationsModule],
  providers: [ItemService],
  bootstrap: [AppComponent]
})
export class AppModule {}
