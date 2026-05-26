import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';

import { SettingsService } from './core/settings/settings.service';
import { CommandPaletteService } from './core/command-palette/command-palette.service';
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
  private readonly palette = inject(CommandPaletteService);

  // ⌘K (macOS) / Ctrl+K (everywhere else) toggles the command palette.
  // Bound at the document level so it fires regardless of focus — including
  // from inside text inputs, by design (convention from Slack / Linear /
  // GitHub: Cmd+K always opens the palette). The service tracks the open
  // ref so consecutive presses close it.
  @HostListener('document:keydown', ['$event'])
  protected onDocKey(event: KeyboardEvent) {
    if (event.key !== 'k' && event.key !== 'K') return;
    const cmd = event.metaKey || event.ctrlKey;
    if (!cmd || event.altKey) return;
    event.preventDefault();
    this.palette.toggle();
  }
}
