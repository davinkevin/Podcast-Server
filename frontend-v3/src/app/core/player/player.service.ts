import { computed, Injectable, signal } from '@angular/core';

import { ItemHAL } from '../models/item.model';

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private readonly current = signal<ItemHAL | undefined>(undefined);

  readonly currentItem = this.current.asReadonly();
  readonly isOpen = computed(() => this.current() !== undefined);
  readonly isVideo = computed(() => {
    const item = this.current();
    return item ? item.mimeType.startsWith('video/') : false;
  });

  open(item: ItemHAL) {
    if (!item.isDownloaded) return;
    this.current.set(item);
  }

  close() {
    this.current.set(undefined);
  }

  /** Close the player if the currently-playing item matches the given id.
   *  Use after a mutation that invalidates playback (reset, delete). */
  closeIf(itemId: string) {
    if (this.current()?.id === itemId) {
      this.close();
    }
  }
}
