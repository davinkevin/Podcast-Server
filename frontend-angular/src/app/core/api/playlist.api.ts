import { inject, Injectable, Signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from '@tanstack/angular-query-experimental';
import { lastValueFrom } from 'rxjs';

import {
  PlaylistsContainerHAL,
  PlaylistWithItemsHAL,
} from '../models/playlist.model';
import { queryKeys } from './query-keys';

@Injectable({ providedIn: 'root' })
export class PlaylistApi {
  private readonly http = inject(HttpClient);
  private readonly queryClient = inject(QueryClient);

  list() {
    return injectQuery(() => ({
      queryKey: queryKeys.playlists.list(),
      queryFn: () =>
        lastValueFrom(
          this.http.get<PlaylistsContainerHAL>('/api/v1/playlists'),
        ),
    }));
  }

  getById(id: Signal<string | undefined>) {
    return injectQuery(() => {
      const v = id();
      return {
        queryKey: v ? queryKeys.playlists.detail(v) : ['playlists', 'detail', 'noop'],
        queryFn: () =>
          lastValueFrom(
            this.http.get<PlaylistWithItemsHAL>(`/api/v1/playlists/${v}`),
          ),
        enabled: !!v,
      };
    });
  }

  createMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { name: string; coverUrl?: string }) => {
        // Always omit `coverUrl` when blank — backend would otherwise try to
        // dereference an empty URI and 500. Only attach when it looks like a
        // real http(s) URL; the backend falls back to its placeholder cover.
        const url = args.coverUrl?.trim();
        const body: Record<string, string> =
          url && /^https?:\/\//.test(url)
            ? { name: args.name, coverUrl: url }
            : { name: args.name };
        return lastValueFrom(
          this.http.post<PlaylistWithItemsHAL>('/api/v1/playlists', body),
        );
      },
      onSuccess: () => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.playlists.list(),
        });
      },
    }));
  }

  deleteMutation() {
    return injectMutation(() => ({
      mutationFn: (id: string) =>
        lastValueFrom(
          this.http.delete(`/api/v1/playlists/${id}`, { responseType: 'text' }),
        ),
      onSuccess: () => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.playlists.all,
        });
      },
    }));
  }

  addItemMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { playlistId: string; itemId: string; podcastId: string }) =>
        lastValueFrom(
          this.http.post<PlaylistWithItemsHAL>(
            `/api/v1/playlists/${args.playlistId}/items/${args.itemId}`,
            null,
          ),
        ),
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.playlists.detail(vars.playlistId),
        });
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.items.playlistsContaining(vars.podcastId, vars.itemId),
        });
      },
    }));
  }

  removeItemMutation() {
    return injectMutation(() => ({
      mutationFn: (args: { playlistId: string; itemId: string; podcastId: string }) =>
        lastValueFrom(
          this.http.delete<PlaylistWithItemsHAL>(
            `/api/v1/playlists/${args.playlistId}/items/${args.itemId}`,
          ),
        ),
      onSuccess: (_data, vars) => {
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.playlists.detail(vars.playlistId),
        });
        this.queryClient.invalidateQueries({
          queryKey: queryKeys.items.playlistsContaining(vars.podcastId, vars.itemId),
        });
      },
    }));
  }
}
