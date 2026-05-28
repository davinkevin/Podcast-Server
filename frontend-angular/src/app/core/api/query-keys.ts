import type { ItemSearchInput } from './item.api';
import type { PodcastItemsInput } from './podcast.api';

/**
 * Single source of truth for TanStack Query keys.
 *
 * Hierarchical so invalidation can target broad groups (e.g.
 * `queryKeys.podcasts.all` invalidates every podcasts-related query) or
 * narrow ones (e.g. `queryKeys.podcasts.detail(id)` only that detail).
 *
 * Convention: every leaf returns a `readonly any[]` so it's directly usable
 * as a `QueryKey`. Functions take only the inputs that influence the result.
 *
 * Phases 1-2 implement the leafs they need; later phases fill the rest in.
 */
export const queryKeys = {
  podcasts: {
    all: ['podcasts'] as const,
    list: () => ['podcasts', 'list'] as const,
    detail: (id: string) => ['podcasts', 'detail', id] as const,
    items: (input: PodcastItemsInput) =>
      ['podcasts', input.podcastId, 'items', input] as const,
  },
  items: {
    all: ['items'] as const,
    search: (input: ItemSearchInput) => ['items', 'search', input] as const,
    detail: (podcastId: string, itemId: string) =>
      ['items', podcastId, itemId] as const,
    playlistsContaining: (podcastId: string, itemId: string) =>
      ['items', podcastId, itemId, 'playlists'] as const,
  },
  playlists: {
    all: ['playlists'] as const,
    list: () => ['playlists', 'list'] as const,
    detail: (id: string) => ['playlists', 'detail', id] as const,
  },
  downloads: {
    all: ['downloads'] as const,
    limit: () => ['downloads', 'limit'] as const,
    numberOfTry: () => ['downloads', 'number-of-try'] as const,
    daysToDownload: () => ['downloads', 'days-to-download'] as const,
  },
  covers: {
    all: ['covers'] as const,
    daysToSave: () => ['covers', 'days-to-save'] as const,
  },
  digest: {
    all: ['digest'] as const,
    within: (within: string, maxItemsPerPodcast: number) =>
      ['digest', within, maxItemsPerPodcast] as const,
  },
  types: ['podcast-types'] as const,
  tags: {
    all: ['tags'] as const,
    search: (name: string) => ['tags', 'search', name] as const,
  },
  buildInfo: ['build-info'] as const,
} as const;
