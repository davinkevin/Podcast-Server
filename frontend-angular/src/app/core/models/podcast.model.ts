export interface PodcastTagHAL {
  readonly id: string;
  readonly name: string;
}

export interface PodcastCoverHAL {
  readonly id: string;
  readonly width: number;
  readonly height: number;
  readonly url: string;
}

export interface PodcastHAL {
  readonly id: string;
  readonly title: string;
  readonly url: string | null;
  readonly hasToBeDeleted: boolean;
  readonly lastUpdate: string | null;
  readonly type: string;
  readonly tags: readonly PodcastTagHAL[];
  readonly cover: PodcastCoverHAL;
}

export interface PodcastsContainerHAL {
  readonly content: readonly PodcastHAL[];
}

/**
 * Body shape of `PUT /api/v1/podcasts/{id}`.
 * Mirrors backend `PodcastUpdateHAL` (id is required in the body in addition to
 * the URL; `type` is intentionally absent — the backend doesn't allow type
 * changes via update, the field is immutable after creation).
 */
export interface PodcastUpdateHAL {
  readonly id: string;
  readonly title: string;
  readonly url: string | null;
  readonly hasToBeDeleted: boolean;
  readonly tags: readonly { readonly id?: string; readonly name: string }[];
  readonly cover: { readonly width: number; readonly height: number; readonly url: string };
}

/** Body shape of `POST /api/v1/podcasts`. Mirrors backend `PodcastCreationHAL`. */
export interface PodcastCreationHAL {
  readonly title: string;
  readonly url: string | null;
  readonly type: string;
  readonly hasToBeDeleted: boolean;
  readonly tags: readonly { readonly id?: string; readonly name: string }[];
  readonly cover: { readonly width: number; readonly height: number; readonly url: string };
}
