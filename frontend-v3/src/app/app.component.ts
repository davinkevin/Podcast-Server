import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';

import { TopbarComponent } from './layout/topbar/topbar.component';
import { SidenavComponent } from './layout/sidenav/sidenav.component';
import { FloatingPlayerComponent } from './layout/floating-player/floating-player.component';

@Component({
  selector: 'ps-root',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule,
    TopbarComponent,
    SidenavComponent,
    FloatingPlayerComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
