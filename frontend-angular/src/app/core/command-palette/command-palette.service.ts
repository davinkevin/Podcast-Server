import { inject, Injectable } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { PodcastApi } from '../api/podcast.api';
import { PlaylistApi } from '../api/playlist.api';
import { CommandPaletteComponent } from './command-palette.component';

@Injectable({ providedIn: 'root' })
export class CommandPaletteService {
  private readonly dialog = inject(MatDialog);
  private readonly podcastApi = inject(PodcastApi);
  private readonly playlistApi = inject(PlaylistApi);
  private ref?: MatDialogRef<CommandPaletteComponent>;

  open() {
    if (this.ref) return;
    // Warm the caches so the palette has navigable items as fast as the
    // network allows. Fire-and-forget: the dialog's `injectQuery` picks
    // the data up reactively as soon as it lands. Within the QueryClient's
    // staleTime (60 s) subsequent opens are instant — `prefetchQuery` is a
    // no-op on fresh cache.
    void this.podcastApi.prefetchList();
    void this.playlistApi.prefetchList();

    this.ref = this.dialog.open(CommandPaletteComponent, {
      width: 'min(640px, 92vw)',
      maxHeight: '70vh',
      position: { top: '15vh' },
      panelClass: 'ps-command-palette-panel',
      // We focus the search input ourselves in the component's
      // afterNextRender, so let Material's autoFocus skip the dialog
      // container (otherwise the focus ring lands on the container, then
      // jumps to the input — visible flicker).
      autoFocus: false,
      restoreFocus: true,
    });
    this.ref.afterClosed().subscribe(() => (this.ref = undefined));
  }

  close() {
    this.ref?.close();
  }

  toggle() {
    if (this.ref) this.close();
    else this.open();
  }
}
