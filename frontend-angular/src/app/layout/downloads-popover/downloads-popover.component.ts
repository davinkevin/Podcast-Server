import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  output,
  signal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  CdkDragDrop,
  DragDropModule,
  moveItemInArray,
} from '@angular/cdk/drag-drop';

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
    DragDropModule,
  ],
  templateUrl: './downloads-popover.component.html',
  styleUrl: './downloads-popover.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadsPopoverComponent {
  protected readonly stream = inject(DownloadStreamService);
  private readonly api = inject(DownloadApi);

  readonly closed = output<void>();

  // Local writable mirror of the SSE queue. Lets us apply optimistic reorders
  // on drop and snap back on error without waiting for the SSE round trip.
  // Effect re-syncs whenever SSE pushes a new queue order.
  protected readonly queue = signal<DownloadingItemHAL[]>([]);

  protected readonly hasContent = computed(
    () => this.stream.downloading().length > 0 || this.queue().length > 0,
  );

  constructor() {
    // `stream.queue()` is readonly; copy into a mutable signal so we can
    // splice optimistically on drop without mutating the SSE state.
    effect(() => this.queue.set([...this.stream.queue()]));
  }

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

  protected onQueueDrop(event: CdkDragDrop<DownloadingItemHAL[]>) {
    if (event.previousIndex === event.currentIndex) return;
    const previous = this.queue();
    const next = previous.slice();
    moveItemInArray(next, event.previousIndex, event.currentIndex);
    this.queue.set(next);
    const moved = next[event.currentIndex];
    this.api.moveInQueue(moved.id, event.currentIndex).subscribe({
      error: () => this.queue.set(previous),
    });
  }

  protected onClearQueue() {
    if (this.queue().length === 0) return;
    if (!confirm('Remove all queued items? Active downloads continue.')) return;
    // Optimistic empty; SSE confirms with a fresh waiting=[] event.
    const previous = this.queue();
    this.queue.set([]);
    this.api.emptyQueue().subscribe({
      error: () => this.queue.set(previous),
    });
  }
}
