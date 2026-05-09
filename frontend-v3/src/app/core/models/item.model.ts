export type ItemStatus =
  | 'NOT_DOWNLOADED'
  | 'STARTED'
  | 'PAUSED'
  | 'STOPPED'
  | 'FAILED'
  | 'FINISH';

export const ITEM_STATUSES: readonly ItemStatus[] = [
  'NOT_DOWNLOADED',
  'STARTED',
  'PAUSED',
  'STOPPED',
  'FAILED',
  'FINISH',
];

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
