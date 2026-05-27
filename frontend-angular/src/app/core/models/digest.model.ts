import { ItemHAL } from './item.model';

export interface DigestCoverHAL {
  readonly id: string;
  readonly width: number;
  readonly height: number;
  readonly url: string;
}

export interface DigestPodcastHAL {
  readonly id: string;
  readonly title: string;
  readonly cover: DigestCoverHAL;
  /** Real total of items published by this podcast within the window. */
  readonly itemCount: number;
  /** The most-recent items, capped server-side by `maxItemsPerPodcast`. */
  readonly items: readonly ItemHAL[];
}

export interface DigestHAL {
  readonly content: readonly DigestPodcastHAL[];
}
