import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DownloadApi } from '../../core/api/download.api';
import { PodcastApi } from '../../core/api/podcast.api';
import { ItemApi } from '../../core/api/item.api';
import { CoverApi } from '../../core/api/cover.api';
import {
  SettingsService,
  ThemePreference,
} from '../../core/settings/settings.service';

const ITEMS_DEFAULT_DAYS = 30;
const COVERS_DEFAULT_DAYS = 365;

@Component({
  selector: 'ps-settings',
  standalone: true,
  imports: [
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export default class SettingsComponent {
  private readonly downloads = inject(DownloadApi);
  private readonly podcasts = inject(PodcastApi);
  private readonly items = inject(ItemApi);
  private readonly covers = inject(CoverApi);
  private readonly settings = inject(SettingsService);
  private readonly snackbar = inject(MatSnackBar);
  protected readonly limitQuery = this.downloads.limit();
  private readonly updateLimitMutation = this.downloads.updateLimitMutation();
  private readonly cleanItemsMutation = this.items.cleanupMutation();
  private readonly cleanCoversMutation = this.covers.cleanupMutation();

  protected readonly limitDraft = signal<number | null>(null);
  protected readonly savingLimit = this.updateLimitMutation.isPending;
  protected readonly limitDirty = computed(() => {
    const draft = this.limitDraft();
    const remote = this.limitQuery.data();
    return draft !== null && remote !== undefined && draft !== remote && draft >= 1;
  });

  protected readonly itemsRetentionDays = signal<number>(ITEMS_DEFAULT_DAYS);
  protected readonly coversRetentionDays = signal<number>(COVERS_DEFAULT_DAYS);
  protected readonly cleaningItems = this.cleanItemsMutation.isPending;
  protected readonly cleaningCovers = this.cleanCoversMutation.isPending;

  protected readonly updating = signal(false);

  protected readonly theme = this.settings.theme;

  constructor() {
    // Mirror the loaded limit into the draft once.
    effect(() => {
      const remote = this.limitQuery.data();
      if (remote !== undefined && this.limitDraft() === null) {
        this.limitDraft.set(remote);
      }
    });
  }

  protected onLimitInput(raw: string) {
    const n = Number.parseInt(raw, 10);
    this.limitDraft.set(Number.isFinite(n) && n >= 1 ? n : null);
  }

  protected onSaveLimit() {
    const v = this.limitDraft();
    if (v === null || !this.limitDirty()) return;
    this.updateLimitMutation.mutate(v, {
      onSuccess: () =>
        this.snackbar.open('Parallel limit updated', undefined, { duration: 2500 }),
      onError: () =>
        this.snackbar.open('Could not update limit', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onCleanItems() {
    const days = this.itemsRetentionDays();
    if (!Number.isFinite(days) || days < 0) return;
    if (!confirm(`Delete downloaded items older than ${days} days?`)) return;
    this.cleanItemsMutation.mutate(days, {
      onSuccess: () =>
        this.snackbar.open('Old items cleaned up', undefined, { duration: 2500 }),
      onError: () =>
        this.snackbar.open('Could not clean items', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onCleanCovers() {
    const days = this.coversRetentionDays();
    if (!Number.isFinite(days) || days < 0) return;
    if (!confirm(`Delete unused covers older than ${days} days?`)) return;
    this.cleanCoversMutation.mutate(days, {
      onSuccess: () =>
        this.snackbar.open('Old covers cleaned up', undefined, { duration: 2500 }),
      onError: () =>
        this.snackbar.open('Could not clean covers', 'Dismiss', { duration: 4000 }),
    });
  }

  protected onUpdateAll(withDownload: boolean) {
    if (this.updating()) return;
    this.updating.set(true);
    this.podcasts.updateAll({ download: withDownload }).subscribe({
      next: () => {
        this.updating.set(false);
        this.snackbar.open(
          withDownload ? 'Update & download started' : 'Update started',
          undefined,
          { duration: 2500 },
        );
      },
      error: () => {
        this.updating.set(false);
        this.snackbar.open('Could not start update', 'Dismiss', { duration: 4000 });
      },
    });
  }

  protected onDownloadOpml() {
    // Backend serves application/xml without Content-Disposition; fetch as blob
    // and synthesize a download with a sensible filename.
    fetch('/api/v1/podcasts/opml')
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.blob();
      })
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'podcast-server.opml';
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
      })
      .catch(() => {
        this.snackbar.open('Could not export OPML', 'Dismiss', { duration: 4000 });
      });
  }

  protected onThemeChange(value: ThemePreference) {
    this.settings.setTheme(value);
  }
}
