export interface PodcastTypeHAL {
  readonly key: string;
  readonly name: string;
}

export interface TypeContainerHAL {
  readonly content: readonly PodcastTypeHAL[];
}
