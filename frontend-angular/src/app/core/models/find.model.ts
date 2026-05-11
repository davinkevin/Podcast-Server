export interface FindCoverInformationHAL {
  readonly width: number;
  readonly height: number;
  readonly url: string;
}

export interface FindPodcastInformationHAL {
  readonly title: string;
  readonly description: string;
  readonly url: string;
  readonly cover: FindCoverInformationHAL | null;
  readonly type: string;
}
