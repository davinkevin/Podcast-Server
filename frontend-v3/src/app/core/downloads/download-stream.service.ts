import { computed, DestroyRef, Injectable, signal } from '@angular/core';

import { DownloadingItemHAL } from '../models/downloading-item.model';

const SSE_URL = '/api/v1/sse';

@Injectable({ providedIn: 'root' })
export class DownloadStreamService {
  private readonly downloadingMap = signal<ReadonlyMap<string, DownloadingItemHAL>>(new Map());
  private readonly queueState = signal<readonly DownloadingItemHAL[]>([]);
  private readonly updatingState = signal<boolean>(false);

  readonly downloading = computed(() => Array.from(this.downloadingMap().values()));
  readonly queue = this.queueState.asReadonly();
  readonly updating = this.updatingState.asReadonly();
  readonly count = computed(() => this.downloading().length + this.queue().length);

  private eventSource?: EventSource;

  constructor(destroyRef: DestroyRef) {
    if (typeof window === 'undefined' || typeof EventSource === 'undefined') {
      return;
    }
    this.connect();
    destroyRef.onDestroy(() => this.eventSource?.close());
  }

  private connect() {
    const es = new EventSource(SSE_URL);

    es.addEventListener('downloading', (ev) => {
      const item = parse<DownloadingItemHAL>(ev);
      if (!item) return;

      this.downloadingMap.update((current) => {
        const next = new Map(current);
        if (item.status === 'STARTED' || item.status === 'PAUSED') {
          next.set(item.id, item);
        } else {
          next.delete(item.id);
        }
        return next;
      });
    });

    es.addEventListener('waiting', (ev) => {
      const items = parse<DownloadingItemHAL[]>(ev);
      this.queueState.set(items ?? []);
    });

    es.addEventListener('updating', (ev) => {
      const flag = parse<boolean>(ev);
      this.updatingState.set(flag === true);
    });

    // EventSource auto-reconnects on transient errors; nothing to do here.
    this.eventSource = es;
  }
}

function parse<T>(ev: Event): T | undefined {
  const data = (ev as MessageEvent<string>).data;
  if (!data) return undefined;
  try {
    return JSON.parse(data) as T;
  } catch {
    return undefined;
  }
}
