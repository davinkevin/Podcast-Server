import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { CoverCardComponent } from '../../shared/cover-card/cover-card.component';
import { MOCK_LIBRARY_ITEMS, MockLibraryItem } from './library.mock';

@Component({
  selector: 'ps-library',
  standalone: true,
  imports: [CoverCardComponent],
  templateUrl: './library.component.html',
  styleUrl: './library.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class LibraryComponent {
  protected readonly items = signal<readonly MockLibraryItem[]>(MOCK_LIBRARY_ITEMS);

  protected readonly cardActions = [
    { label: 'Open podcast', icon: 'open_in_new' },
    { label: 'Add to playlist', icon: 'playlist_add' },
    { label: 'Download', icon: 'download' },
  ] as const;

  protected onPlay(item: MockLibraryItem) {
    // PR 5: route into PlayerService.
    console.debug('[library] play', item.id);
  }

  protected onOpen(item: MockLibraryItem) {
    // PR 5: navigate to /podcasts/:id/items/:itemId.
    console.debug('[library] open', item.id);
  }
}
