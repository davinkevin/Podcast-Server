import { computed, Injectable, signal } from '@angular/core';

import { ItemHAL } from '../models/item.model';

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private readonly current = signal<ItemHAL | undefined>(undefined);
  private readonly expanded = signal<boolean>(false);

  readonly currentItem = this.current.asReadonly();
  readonly isOpen = computed(() => this.current() !== undefined);
  readonly isExpanded = this.expanded.asReadonly();
  readonly isVideo = computed(() => {
    const item = this.current();
    return item ? item.mimeType.startsWith('video/') : false;
  });

  open(item: ItemHAL) {
    if (!item.isDownloaded) return;
    this.current.set(item);
    // Video items open expanded so the user actually sees the video;
    // audio defaults to the mini bar at the bottom.
    this.expanded.set(item.mimeType.startsWith('video/'));
  }

  close() {
    this.current.set(undefined);
    this.expanded.set(false);
  }

  /** Close the player if the currently-playing item matches the given id.
   *  Use after a mutation that invalidates playback (reset, delete). */
  closeIf(itemId: string) {
    if (this.current()?.id === itemId) {
      this.close();
    }
  }

  toggleExpanded() {
    this.expanded.update((v) => !v);
  }

  collapse() {
    this.expanded.set(false);
  }
}
