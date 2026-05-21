import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { FindApi } from '../../core/api/find.api';
import { PodcastApi } from '../../core/api/podcast.api';
import { FindPodcastInformationHAL } from '../../core/models/find.model';
import { PodcastCreationHAL, PodcastHAL } from '../../core/models/podcast.model';

const FIND_DEBOUNCE_MS = 500;

type Mode = 'url' | 'manual';

@Component({
  selector: 'ps-podcast-create-dialog',
  standalone: true,
  imports: [
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Add podcast</h2>
    <mat-dialog-content class="dialog">
      @if (mode() === 'url') {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Source URL</mat-label>
          <input
            matInput
            type="url"
            autofocus
            placeholder="Paste a feed URL…"
            [ngModel]="urlInput()"
            (ngModelChange)="urlInput.set($event)"
            [disabled]="creating()"
          />
          @if (urlInput()) {
            <button
              mat-icon-button
              matSuffix
              type="button"
              aria-label="Clear"
              (click)="urlInput.set('')"
            >
              <mat-icon>close</mat-icon>
            </button>
          }
        </mat-form-field>

        @let info = findInfo();
        @let findErr = findError();
        <div class="dialog__preview">
          @if (finding()) {
            <div class="preview preview--loading">
              <mat-spinner diameter="20" />
              <span>Detecting podcast…</span>
            </div>
          } @else if (findErr) {
            <div class="preview preview--error">
              <mat-icon>error_outline</mat-icon>
              <span>{{ findErr }}</span>
            </div>
          } @else if (info) {
            <div class="preview">
              @if (info.cover; as c) {
                <img class="preview__cover" [src]="c.url" [alt]="info.title" />
              }
              <div class="preview__meta">
                <div class="preview__title">{{ info.title }}</div>
                <div class="preview__sub">{{ info.type }}</div>
              </div>
            </div>
          } @else {
            <div class="preview preview--empty">
              <mat-icon>search</mat-icon>
              <span>Paste a URL to detect title, type and cover.</span>
            </div>
          }
        </div>
      } @else {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Title</mat-label>
          <input
            matInput
            required
            [ngModel]="manualTitle()"
            (ngModelChange)="manualTitle.set($event)"
            [disabled]="creating()"
          />
        </mat-form-field>

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Cover URL</mat-label>
          <input
            matInput
            type="url"
            [ngModel]="manualCoverUrl()"
            (ngModelChange)="manualCoverUrl.set($event)"
            [disabled]="creating()"
          />
          <mat-hint>Paste any image URL.</mat-hint>
        </mat-form-field>

        @if (manualCover().url) {
          <div class="dialog__preview">
            <div class="preview">
              <img class="preview__cover" [src]="manualCover().url" alt="" />
              <div class="preview__meta">
                <div class="preview__title">{{ manualTitle() || 'New podcast' }}</div>
                <div class="preview__sub">Upload</div>
              </div>
            </div>
          </div>
        }
      }

      <mat-slide-toggle
        [checked]="hasToBeDeleted()"
        (change)="hasToBeDeleted.set($event.checked)"
      >
        Auto-delete old downloaded episodes
      </mat-slide-toggle>

      @if (mode() === 'url') {
        <button
          mat-button
          type="button"
          class="dialog__mode-switch"
          (click)="mode.set('manual')"
        >
          Or add an upload-only podcast
        </button>
      } @else {
        <button
          mat-button
          type="button"
          class="dialog__mode-switch"
          (click)="mode.set('url')"
        >
          Or add a podcast from a feed URL
        </button>
      }
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button [disabled]="creating()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="!canCreate() || creating()"
        (click)="onCreate()"
      >
        @if (creating()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>add</mat-icon>
        }
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .dialog {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      min-width: min(32rem, 90vw);
    }
    .dialog > :first-child {
      margin-top: 1.5rem;
    }
    .dialog mat-slide-toggle {
      margin-top: 0.25rem;
    }
    .dialog__mode-switch {
      align-self: flex-start;
      margin-left: -8px;
    }
    .dialog__preview {
      background: var(--mat-sys-surface-container-low);
      border-radius: 16px;
      padding: 0.75rem 1rem;
      min-height: 80px;
      display: flex;
      align-items: center;
    }
    .preview {
      display: flex;
      align-items: center;
      gap: 1rem;
      width: 100%;
    }
    .preview--loading,
    .preview--empty {
      color: var(--mat-sys-on-surface-variant);
      font: var(--mat-sys-body-medium);
    }
    .preview--error {
      color: var(--mat-sys-error);
      font: var(--mat-sys-body-medium);
    }
    .preview__cover {
      width: 64px;
      height: 64px;
      object-fit: cover;
      border-radius: 12px;
      flex: 0 0 auto;
      display: block;
    }
    .preview__meta {
      flex: 1 1 auto;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .preview__title {
      font: var(--mat-sys-title-small);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .preview__sub {
      font: var(--mat-sys-body-small);
      color: var(--mat-sys-on-surface-variant);
      text-transform: capitalize;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PodcastCreateDialogComponent {
  private readonly findApi = inject(FindApi);
  private readonly podcastApi = inject(PodcastApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<PodcastCreateDialogComponent, PodcastHAL>);

  protected readonly mode = signal<Mode>('url');

  // ── url mode state ────────────────────────────────────────────────────
  protected readonly urlInput = signal<string>('');
  protected readonly findInfo = signal<FindPodcastInformationHAL | undefined>(undefined);
  protected readonly findError = signal<string | undefined>(undefined);
  protected readonly finding = signal(false);

  // ── manual mode state ─────────────────────────────────────────────────
  protected readonly manualTitle = signal<string>('');
  protected readonly manualCoverUrl = signal<string>('');
  protected readonly manualCover = signal<{ width: number; height: number; url: string }>(
    { width: 0, height: 0, url: '' },
  );

  // ── shared ────────────────────────────────────────────────────────────
  private readonly createMutation = this.podcastApi.createMutation();
  protected readonly creating = this.createMutation.isPending;
  protected readonly hasToBeDeleted = signal(true);

  protected readonly canCreate = computed(() =>
    this.mode() === 'url'
      ? !!this.findInfo()
      : !!this.manualTitle().trim() && !!this.manualCover().url,
  );

  private pendingFind: ReturnType<typeof setTimeout> | undefined;
  private pendingCoverDims: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // url mode: paste URL → /find with debounce.
    effect(() => {
      const url = this.urlInput().trim();
      if (this.pendingFind) {
        clearTimeout(this.pendingFind);
        this.pendingFind = undefined;
      }
      if (!isLikelyUrl(url)) {
        this.findInfo.set(undefined);
        this.findError.set(undefined);
        this.finding.set(false);
        return;
      }
      this.finding.set(true);
      this.findError.set(undefined);
      this.pendingFind = setTimeout(() => {
        this.findApi.byUrl(url).subscribe({
          next: (info) => {
            this.findInfo.set(info);
            this.finding.set(false);
          },
          error: () => {
            this.findInfo.set(undefined);
            this.findError.set('Could not parse this URL.');
            this.finding.set(false);
          },
        });
      }, FIND_DEBOUNCE_MS);
    });

    // manual mode: paste cover URL → infer width/height client-side.
    effect(() => {
      const url = this.manualCoverUrl().trim();
      if (this.pendingCoverDims) {
        clearTimeout(this.pendingCoverDims);
        this.pendingCoverDims = undefined;
      }
      if (!isLikelyUrl(url)) {
        this.manualCover.set({ width: 0, height: 0, url: '' });
        return;
      }
      this.pendingCoverDims = setTimeout(() => {
        if (typeof Image === 'undefined') {
          this.manualCover.set({ width: 1000, height: 1000, url });
          return;
        }
        const img = new Image();
        img.onload = () =>
          this.manualCover.set({
            width: img.naturalWidth || 1000,
            height: img.naturalHeight || 1000,
            url,
          });
        img.onerror = () => this.manualCover.set({ width: 1000, height: 1000, url });
        img.src = url;
      }, FIND_DEBOUNCE_MS);
    });
  }

  protected onCancel() {
    this.dialogRef.close();
  }

  protected onCreate() {
    const body: PodcastCreationHAL | null = this.buildBody();
    if (!body) return;
    this.createMutation.mutate(body, {
      onSuccess: (created) => {
        // Kick off a refresh right after creation so the new podcast fills
        // with episodes without an extra manual click. Driven from the
        // client so the create endpoint stays free of side-effects.
        // Skipped for upload-only podcasts — there's no remote feed to
        // fetch.
        if (created.type !== 'upload') {
          this.podcastApi.triggerUpdate(created.id).subscribe();
        }
        this.snackbar.open('Podcast created', undefined, { duration: 2500 });
        this.dialogRef.close(created);
      },
      onError: () =>
        this.snackbar.open('Could not create podcast', 'Dismiss', { duration: 4000 }),
    });
  }

  private buildBody(): PodcastCreationHAL | null {
    if (this.mode() === 'url') {
      const info = this.findInfo();
      if (!info) return null;
      if (!info.cover) {
        this.snackbar.open('No cover available — try a different URL.', 'Dismiss', {
          duration: 4000,
        });
        return null;
      }
      return {
        title: info.title,
        url: info.url,
        type: info.type,
        hasToBeDeleted: this.hasToBeDeleted(),
        tags: [],
        cover: info.cover,
      };
    }
    const c = this.manualCover();
    return {
      title: this.manualTitle().trim(),
      url: null,
      type: 'upload',
      hasToBeDeleted: this.hasToBeDeleted(),
      tags: [],
      cover: { width: c.width, height: c.height, url: c.url },
    };
  }
}

function isLikelyUrl(s: string): boolean {
  return /^https?:\/\/\S+\.\S+/.test(s);
}
