import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DownloadApi } from '../../core/api/download.api';
import { ItemApi } from '../../core/api/item.api';
import { CoverApi } from '../../core/api/cover.api';
import { BuildInfoApi } from '../../core/api/build-info.api';
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
    DatePipe,
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
  private readonly items = inject(ItemApi);
  private readonly covers = inject(CoverApi);
  private readonly buildInfoApi = inject(BuildInfoApi);
  private readonly settings = inject(SettingsService);
  private readonly snackbar = inject(MatSnackBar);
  protected readonly limitQuery = this.downloads.limit();
  protected readonly numberOfTryQuery = this.downloads.numberOfTry();
  protected readonly daysToDownloadQuery = this.downloads.daysToDownload();
  protected readonly daysToSaveQuery = this.covers.daysToSave();
  protected readonly buildInfoQuery = this.buildInfoApi.info();
  private readonly updateLimitMutation = this.downloads.updateLimitMutation();
  private readonly updateNumberOfTryMutation = this.downloads.updateNumberOfTryMutation();
  private readonly updateDaysToDownloadMutation = this.downloads.updateDaysToDownloadMutation();
  private readonly updateDaysToSaveMutation = this.covers.updateDaysToSaveMutation();
  private readonly cleanItemsMutation = this.items.cleanupMutation();
  private readonly cleanCoversMutation = this.covers.cleanupMutation();

  protected readonly limitDraft = signal<number | null>(null);
  protected readonly numberOfTryDraft = signal<number | null>(null);
  protected readonly daysToDownloadDraft = signal<number | null>(null);
  protected readonly daysToSaveDraft = signal<number | null>(null);

  private readonly limitDirty = computed(() =>
    this.isDirty(this.limitDraft(), this.limitQuery.data(), 1),
  );
  private readonly numberOfTryDirty = computed(() =>
    this.isDirty(this.numberOfTryDraft(), this.numberOfTryQuery.data(), 0),
  );
  private readonly daysToDownloadDirty = computed(() =>
    this.isDirty(this.daysToDownloadDraft(), this.daysToDownloadQuery.data(), 0),
  );
  private readonly daysToSaveDirty = computed(() =>
    this.isDirty(this.daysToSaveDraft(), this.daysToSaveQuery.data(), 0),
  );

  protected readonly downloadDirty = computed(
    () =>
      this.limitDirty() ||
      this.numberOfTryDirty() ||
      this.daysToDownloadDirty() ||
      this.daysToSaveDirty(),
  );

  protected readonly savingDownload = computed(
    () =>
      this.updateLimitMutation.isPending() ||
      this.updateNumberOfTryMutation.isPending() ||
      this.updateDaysToDownloadMutation.isPending() ||
      this.updateDaysToSaveMutation.isPending(),
  );

  protected readonly itemsRetentionDays = signal<number>(ITEMS_DEFAULT_DAYS);
  protected readonly coversRetentionDays = signal<number>(COVERS_DEFAULT_DAYS);
  protected readonly cleaningItems = this.cleanItemsMutation.isPending;
  protected readonly cleaningCovers = this.cleanCoversMutation.isPending;

  protected readonly theme = this.settings.theme;

  constructor() {
    this.mirrorOnce(this.limitQuery.data, this.limitDraft);
    this.mirrorOnce(this.numberOfTryQuery.data, this.numberOfTryDraft);
    this.mirrorOnce(this.daysToDownloadQuery.data, this.daysToDownloadDraft);
    this.mirrorOnce(this.daysToSaveQuery.data, this.daysToSaveDraft);
  }

  private mirrorOnce(
    source: () => number | undefined,
    target: ReturnType<typeof signal<number | null>>,
  ) {
    effect(() => {
      const remote = source();
      if (remote !== undefined && target() === null) target.set(remote);
    });
  }

  private isDirty(draft: number | null, remote: number | undefined, min: number) {
    return draft !== null && remote !== undefined && draft !== remote && draft >= min;
  }

  private parseInputAs(min: number) {
    return (raw: string) => {
      const n = Number.parseInt(raw, 10);
      return Number.isFinite(n) && n >= min ? n : null;
    };
  }

  protected onLimitInput(raw: string) {
    this.limitDraft.set(this.parseInputAs(1)(raw));
  }

  protected onNumberOfTryInput(raw: string) {
    this.numberOfTryDraft.set(this.parseInputAs(0)(raw));
  }

  protected onDaysToDownloadInput(raw: string) {
    this.daysToDownloadDraft.set(this.parseInputAs(0)(raw));
  }

  protected onDaysToSaveInput(raw: string) {
    this.daysToSaveDraft.set(this.parseInputAs(0)(raw));
  }

  protected onSaveDownload() {
    const pending: Promise<unknown>[] = [];
    if (this.limitDirty())
      pending.push(this.updateLimitMutation.mutateAsync(this.limitDraft()!));
    if (this.numberOfTryDirty())
      pending.push(this.updateNumberOfTryMutation.mutateAsync(this.numberOfTryDraft()!));
    if (this.daysToDownloadDirty())
      pending.push(
        this.updateDaysToDownloadMutation.mutateAsync(this.daysToDownloadDraft()!),
      );
    if (this.daysToSaveDirty())
      pending.push(this.updateDaysToSaveMutation.mutateAsync(this.daysToSaveDraft()!));
    if (pending.length === 0) return;
    Promise.all(pending)
      .then(() =>
        this.snackbar.open('Download settings updated', undefined, { duration: 2500 }),
      )
      .catch(() =>
        this.snackbar.open('Could not update download settings', 'Dismiss', {
          duration: 4000,
        }),
      );
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
