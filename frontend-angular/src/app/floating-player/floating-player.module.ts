import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule, MatToolbarModule } from '@angular/material';
import { FloatingPlayerComponent } from '#app/floating-player/floating-player.component';
import { ToolbarComponent } from '#app/shared/toolbar/toolbar.component';

@NgModule({
	imports: [/* Core */ CommonModule, /* Material */ MatToolbarModule, MatIconModule],
	exports: [FloatingPlayerComponent],
	declarations: [FloatingPlayerComponent]
})
export class FloatingPlayerModule {}
