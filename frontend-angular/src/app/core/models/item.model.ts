/**
 * Every status the backend's `Status` enum can send or accept, in its order.
 * This array is the single source of truth: `ItemStatus` is derived from it, so
 * the two can never drift — and code that needs "every status except X" can
 * filter it instead of re-listing values by hand.
 *
 * `PAUSED` is legacy: no backend path writes it any more, only a startup
 * cleanup reads it back. It stays here because a filter derived from this list
 * then keeps matching rows written by older versions at no cost.
 */
export const ITEM_STATUSES = [
  'NOT_DOWNLOADED',
  'STARTED',
  'PAUSED',
  'DELETED',
  'STOPPED',
  'FAILED',
  'FINISH',
] as const;

export type ItemStatus = (typeof ITEM_STATUSES)[number];

export interface ItemPodcastRefHAL {
  readonly id: string;
  readonly title: string;
  readonly url: string;
}

export interface ItemCoverHAL {
  readonly id: string;
  readonly width: number;
  readonly height: number;
  readonly url: string;
  readonly proxyURL: string;
}

export interface ItemHAL {
  readonly id: string;
  readonly title: string;
  readonly url: string;
  readonly pubDate: string | null;
  readonly downloadDate: string | null;
  readonly creationDate: string;
  readonly description: string;
  readonly mimeType: string;
  readonly length: number | null;
  readonly fileName: string | null;
  readonly status: ItemStatus;
  readonly podcast: ItemPodcastRefHAL;
  readonly cover: ItemCoverHAL;
  readonly isDownloaded: boolean;
  readonly podcastId: string;
  readonly proxyURL: string;
}
