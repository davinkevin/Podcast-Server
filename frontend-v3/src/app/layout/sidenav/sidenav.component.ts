import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ConnectedPosition, OverlayModule } from '@angular/cdk/overlay';

import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { SettingsService } from '../../core/settings/settings.service';
import { DownloadsPopoverComponent } from '../downloads-popover/downloads-popover.component';

interface NavLink {
  readonly label: string;
  readonly icon: string;
  readonly path: string;
}

const TOP_LINKS: readonly NavLink[] = [
  { label: 'Library', icon: 'library_music', path: '/library' },
  { label: 'Podcasts', icon: 'podcasts', path: '/podcasts' },
  { label: 'Playlists', icon: 'playlist_play', path: '/playlists' },
];

const BOTTOM_LINKS: readonly NavLink[] = [
  { label: 'Settings', icon: 'settings', path: '/settings' },
];

const DOWNLOADS_OVERLAY_POSITIONS: ConnectedPosition[] = [
  // Downloads lives near the bottom of the sidenav, so prefer opening upward
  // (align the popover's bottom with the item's bottom).
  {
    originX: 'end',
    originY: 'bottom',
    overlayX: 'start',
    overlayY: 'bottom',
    offsetX: 8,
  },
  // Fallback: open downward (used if there isn't enough room above, e.g.
  // very short viewports).
  {
    originX: 'end',
    originY: 'top',
    overlayX: 'start',
    overlayY: 'top',
    offsetX: 8,
  },
];

@Component({
  selector: 'ps-sidenav',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatTooltipModule,
    OverlayModule,
    DownloadsPopoverComponent,
  ],
  templateUrl: './sidenav.component.html',
  styleUrl: './sidenav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidenavComponent {
  private readonly settings = inject(SettingsService);

  protected readonly topLinks = TOP_LINKS;
  protected readonly bottomLinks = BOTTOM_LINKS;
  protected readonly downloadCount = inject(DownloadStreamService).count;
  protected readonly downloadsOpen = signal(false);
  protected readonly downloadsOverlayPositions = DOWNLOADS_OVERLAY_POSITIONS;

  protected readonly isRail = computed(() => this.settings.sidenavMode() === 'rail');

  protected toggleDownloads() {
    this.downloadsOpen.update((v) => !v);
  }

  protected closeDownloads() {
    this.downloadsOpen.set(false);
  }

  protected toggleRail(event: MouseEvent) {
    this.settings.toggleSidenavMode();
    // Mouse click leaves the button focused, and Material's focus state layer
    // reads visually as a stuck hover — blur so the resting state is clean.
    (event.currentTarget as HTMLElement | null)?.blur();
  }
}
