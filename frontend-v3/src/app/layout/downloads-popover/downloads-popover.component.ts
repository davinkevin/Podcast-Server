import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  output,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { DownloadStreamService } from '../../core/downloads/download-stream.service';
import { DownloadApi } from '../../core/api/download.api';
import { DownloadingItemHAL } from '../../core/models/downloading-item.model';

@Component({
  selector: 'ps-downloads-popover',
  standalone: true,
  imports: [
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  templateUrl: './downloads-popover.component.html',
  styleUrl: './downloads-popover.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadsPopoverComponent {
  protected readonly stream = inject(DownloadStreamService);
  private readonly api = inject(DownloadApi);

  readonly close = output<void>();

  protected readonly hasContent = computed(
    () => this.stream.downloading().length > 0 || this.stream.queue().length > 0,
  );

  protected progressMode(item: DownloadingItemHAL): 'determinate' | 'indeterminate' {
    return item.progression > 0 ? 'determinate' : 'indeterminate';
  }

  protected onStopOne(item: DownloadingItemHAL) {
    this.api.stopOne(item.id).subscribe();
  }

  protected onRemoveFromQueue(item: DownloadingItemHAL) {
    this.api.removeFromQueue(item.id).subscribe();
  }

  protected onStopAll() {
    this.api.stopAll().subscribe();
  }
}
