import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { QueryClient } from '@tanstack/angular-query-experimental';

import { DownloadingItemHAL } from '../models/downloading-item.model';
import { queryKeys } from '../api/query-keys';

const SSE_URL = '/api/v1/sse';

@Injectable({ providedIn: 'root' })
export class DownloadStreamService {
  private readonly queryClient = inject(QueryClient);
  private readonly downloadingMap = signal<ReadonlyMap<string, DownloadingItemHAL>>(new Map());
  private readonly queueState = signal<readonly DownloadingItemHAL[]>([]);
  private readonly updatingState = signal<boolean>(false);
  // Set of podcast ids currently being refreshed. Pushed via the
  // `podcast-updating` SSE event so the UI can spin the "Update now"
  // button on the exact /podcasts/:id page that triggered the run.
  private readonly updatingPodcastsState = signal<ReadonlySet<string>>(new Set());

  readonly downloading = computed(() => Array.from(this.downloadingMap().values()));
  readonly queue = this.queueState.asReadonly();
  readonly updating = this.updatingState.asReadonly();
  readonly updatingPodcasts = this.updatingPodcastsState.asReadonly();
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

      const inProgress = item.status === 'STARTED' || item.status === 'PAUSED';

      this.downloadingMap.update((current) => {
        const next = new Map(current);
        if (inProgress) {
          next.set(item.id, item);
        } else {
          next.delete(item.id);
        }
        return next;
      });

      // Once a download has settled (FINISH/FAILED/STOPPED), refetch any
      // visible items list so the row picks up the new `isDownloaded`
      // (and the item-detail page picks up its new status).
      if (!inProgress) {
        this.queryClient.invalidateQueries({ queryKey: queryKeys.items.all });
        this.queryClient.invalidateQueries({
          queryKey: ['podcasts', item.podcast.id, 'items'],
        });
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.items.detail(item.podcast.id, item.id),
        });
      }
    });

    es.addEventListener('waiting', (ev) => {
      const items = parse<DownloadingItemHAL[]>(ev);
      this.queueState.set(items ?? []);
    });

    es.addEventListener('updating', (ev) => {
      const flag = parse<boolean>(ev);
      this.updatingState.set(flag === true);
    });

    es.addEventListener('podcast-updating', (ev) => {
      const payload = parse<{ podcastId: string; updating: boolean }>(ev);
      if (!payload) return;
      this.updatingPodcastsState.update((current) => {
        const next = new Set(current);
        if (payload.updating) next.add(payload.podcastId);
        else next.delete(payload.podcastId);
        return next;
      });
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
