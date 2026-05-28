import { computed, Injectable, signal } from '@angular/core';

/**
 * The minimum shape required to play and surface an item in the floating
 * player + queue. Intersect of `ItemHAL` (loaded from podcast/item routes)
 * and `PlaylistItemHAL` (loaded from playlist routes) so the queue can
 * mix and match without coercion.
 */
export type Playable = {
  readonly id: string;
  readonly title: string;
  readonly proxyURL: string;
  readonly mimeType: string;
  readonly isDownloaded: boolean;
  readonly podcast: { readonly id: string; readonly title: string };
  readonly cover: { readonly url: string; readonly proxyURL: string };
};

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private readonly current = signal<Playable | undefined>(undefined);
  // Queue mirrors the source list (e.g. playlist items) so the user can
  // navigate forward / backward. `currentIndex` points inside it.
  // A solo `open(item)` populates a single-element queue so the controls
  // behave uniformly — prev/next are simply disabled at the edges.
  private readonly queueState = signal<readonly Playable[]>([]);
  private readonly indexState = signal(0);

  readonly currentItem = this.current.asReadonly();
  readonly queue = this.queueState.asReadonly();
  readonly currentIndex = this.indexState.asReadonly();
  readonly isOpen = computed(() => this.current() !== undefined);
  readonly isVideo = computed(() => {
    const item = this.current();
    return item ? item.mimeType.startsWith('video/') : false;
  });
  readonly hasPrev = computed(() => this.indexState() > 0);
  readonly hasNext = computed(
    () => this.indexState() < this.queueState().length - 1,
  );

  open(item: Playable) {
    if (!item.isDownloaded) return;
    this.queueState.set([item]);
    this.indexState.set(0);
    this.current.set(item);
  }

  /**
   * Replace the queue with `items` and play the entry at `startIndex`.
   * Skips silently if that entry isn't downloaded — playable items are a
   * subset of the playlist (the queue still surfaces every item, but
   * non-playable ones are visible-only).
   */
  playFromList(items: readonly Playable[], startIndex: number) {
    if (items.length === 0) return;
    const idx = Math.max(0, Math.min(startIndex, items.length - 1));
    const item = items[idx];
    if (!item.isDownloaded) return;
    this.queueState.set(items);
    this.indexState.set(idx);
    this.current.set(item);
  }

  /** Advance to the next downloaded item; skips over non-playable ones. */
  next() {
    const q = this.queueState();
    for (let i = this.indexState() + 1; i < q.length; i++) {
      if (q[i].isDownloaded) {
        this.indexState.set(i);
        this.current.set(q[i]);
        return;
      }
    }
  }

  /** Go back to the previous downloaded item; skips over non-playable ones. */
  prev() {
    const q = this.queueState();
    for (let i = this.indexState() - 1; i >= 0; i--) {
      if (q[i].isDownloaded) {
        this.indexState.set(i);
        this.current.set(q[i]);
        return;
      }
    }
  }

  /** Jump to a specific entry in the queue (from the queue popover). */
  jumpTo(index: number) {
    const q = this.queueState();
    if (index < 0 || index >= q.length) return;
    const item = q[index];
    if (!item.isDownloaded) return;
    this.indexState.set(index);
    this.current.set(item);
  }

  /**
   * Insert `item` right after the currently-playing entry so it plays
   * immediately when the current one finishes. If the player is closed
   * (nothing playing), just start the item — same effect with one less
   * step for the user. No-op if the item is already in the queue (the
   * UI exposes a "Remove from queue" action instead).
   */
  playNext(item: Playable) {
    if (!this.isOpen()) {
      this.open(item);
      return;
    }
    if (this.isQueued(item.id)) return;
    const q = [...this.queueState()];
    q.splice(this.indexState() + 1, 0, item);
    this.queueState.set(q);
  }

  /**
   * Append `item` to the end of the queue. Falls back to `open` when
   * nothing is playing so the user always gets immediate feedback.
   * No-op if the item is already in the queue.
   */
  enqueue(item: Playable) {
    if (!this.isOpen()) {
      this.open(item);
      return;
    }
    if (this.isQueued(item.id)) return;
    this.queueState.update((q) => [...q, item]);
  }

  /**
   * Remove an item from the queue. No-op when the item isn't queued or
   * when it's the currently-playing one — for that case the user should
   * call `close()` or skip with `next()`/`prev()`. If the removed item
   * sits BEFORE `currentIndex`, the index is decremented so the current
   * entry keeps playing rather than the queue snapping forward.
   */
  dequeue(id: string) {
    const q = this.queueState();
    const idx = q.findIndex((i) => i.id === id);
    if (idx < 0) return;
    const currentIdx = this.indexState();
    if (idx === currentIdx) return;
    this.queueState.set(q.slice(0, idx).concat(q.slice(idx + 1)));
    if (idx < currentIdx) {
      this.indexState.set(currentIdx - 1);
    }
  }

  /** True when an item with this id is somewhere in the current queue
   *  (including the currently-playing entry). */
  isQueued(id: string): boolean {
    return this.queueState().some((i) => i.id === id);
  }

  close() {
    this.current.set(undefined);
    this.queueState.set([]);
    this.indexState.set(0);
  }

  /** Close the player if the currently-playing item matches the given id.
   *  Use after a mutation that invalidates playback (reset, delete). */
  closeIf(itemId: string) {
    if (this.current()?.id === itemId) {
      this.close();
    }
  }
}
