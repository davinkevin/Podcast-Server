export interface PlaylistHAL {
  readonly id: string;
  readonly name: string;
}

export interface PlaylistsContainerHAL {
  readonly content: readonly PlaylistHAL[];
}

export interface PlaylistItemHAL {
  readonly id: string;
  readonly title: string;
  readonly url: string | null;
  readonly proxyURL: string;
  readonly description: string | null;
  readonly mimeType: string;
  readonly isDownloaded: boolean;
  readonly podcast: { readonly id: string; readonly title: string };
  readonly cover: {
    readonly id: string;
    readonly width: number;
    readonly height: number;
    readonly url: string;
    readonly proxyURL: string;
  };
}

export interface PlaylistWithItemsHAL {
  readonly id: string;
  readonly name: string;
  readonly items: readonly PlaylistItemHAL[];
}
