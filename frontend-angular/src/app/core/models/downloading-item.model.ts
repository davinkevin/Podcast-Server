import { ItemStatus } from './item.model';

export interface DownloadingItemHAL {
  readonly id: string;
  readonly title: string;
  readonly status: ItemStatus;
  readonly url: string;
  readonly progression: number;
  readonly isDownloaded: boolean;
  readonly podcast: { readonly id: string; readonly title: string };
  readonly cover: { readonly id: string; readonly url: string };
}
