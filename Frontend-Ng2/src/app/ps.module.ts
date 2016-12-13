import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule } from '@angular/material';
import { PsComponent } from './ps.component';
import {ItemService} from './shared/item/item.service';

@NgModule({
  declarations: [
    PsComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule,
    MaterialModule.forRoot()
  ],
  providers: [ItemService],
  bootstrap: [PsComponent]
})
export class AppModule {}
