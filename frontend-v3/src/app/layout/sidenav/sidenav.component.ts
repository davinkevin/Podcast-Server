import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { ConnectedPosition, OverlayModule } from '@angular/cdk/overlay';

import { DownloadStreamService } from '../../core/downloads/download-stream.service';
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
    MatBadgeModule,
    OverlayModule,
    DownloadsPopoverComponent,
  ],
  templateUrl: './sidenav.component.html',
  styleUrl: './sidenav.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidenavComponent {
  protected readonly topLinks = TOP_LINKS;
  protected readonly bottomLinks = BOTTOM_LINKS;
  protected readonly downloadCount = inject(DownloadStreamService).count;
  protected readonly downloadsOpen = signal(false);
  protected readonly downloadsOverlayPositions = DOWNLOADS_OVERLAY_POSITIONS;

  protected toggleDownloads() {
    this.downloadsOpen.update((v) => !v);
  }

  protected closeDownloads() {
    this.downloadsOpen.set(false);
  }
}
