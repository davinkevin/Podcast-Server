import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  ConnectedPosition,
  OverlayModule,
} from '@angular/cdk/overlay';

import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { DownloadsPopoverComponent } from '../downloads-popover/downloads-popover.component';

const DOWNLOADS_OVERLAY_POSITIONS: ConnectedPosition[] = [
  {
    originX: 'end',
    originY: 'bottom',
    overlayX: 'end',
    overlayY: 'top',
    offsetY: 8,
  },
];

@Component({
  selector: 'ps-topbar',
  standalone: true,
  imports: [
    RouterLink,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatMenuModule,
    MatTooltipModule,
    OverlayModule,
    DownloadsPopoverComponent,
  ],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  protected readonly downloadCount = inject(DownloadStreamService).count;
  protected readonly downloadsOpen = signal(false);
  protected readonly overlayPositions = DOWNLOADS_OVERLAY_POSITIONS;

  protected toggleDownloads() {
    this.downloadsOpen.update((v) => !v);
  }

  protected closeDownloads() {
    this.downloadsOpen.set(false);
  }
}
