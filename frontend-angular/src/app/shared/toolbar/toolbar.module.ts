import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToolbarComponent } from './toolbar.component';
import { MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule } from '@angular/material';

@NgModule({
	imports: [CommonModule, MatIconModule, MatButtonModule, MatMenuModule, MatToolbarModule],
	exports: [ToolbarComponent],
	declarations: [ToolbarComponent]
})
export class ToolbarModule {}
