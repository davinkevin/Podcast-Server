import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule, MatToolbarModule } from '@angular/material';
import { FloatingPlayerComponent } from '#app/floating-player/floating-player.component';
import { floatingPlayer } from '#app/floating-player/floating-player.reducer';
import { StoreModule } from '@ngrx/store';

@NgModule({
	imports: [
		/* Core */ CommonModule,
		/* Material */ MatToolbarModule,
		MatIconModule,
		/* NgRx */ StoreModule.forFeature('floatingPlayer', floatingPlayer)
	],
	exports: [FloatingPlayerComponent],
	declarations: [FloatingPlayerComponent]
})
export class FloatingPlayerModule {}
