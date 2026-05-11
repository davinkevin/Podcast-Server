import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';

import { SettingsService } from './core/settings/settings.service';
import { SidenavComponent } from './layout/sidenav/sidenav.component';
import { FloatingPlayerComponent } from './layout/floating-player/floating-player.component';

@Component({
  selector: 'ps-root',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule,
    SidenavComponent,
    FloatingPlayerComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  protected readonly settings = inject(SettingsService);
}
