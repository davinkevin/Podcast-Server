import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
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
import { PodcastHAL, PodcastUpdateHAL } from '../../core/models/podcast.model';
import { TagInput } from '../../core/models/tag.model';
import { TagsFieldComponent } from '../../shared/tags-field/tags-field.component';

const FIND_DEBOUNCE_MS = 500;

@Component({
  selector: 'ps-podcast-edit-dialog',
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
    TagsFieldComponent,
  ],
  template: `
    <h2 mat-dialog-title>Edit podcast</h2>
    <mat-dialog-content class="dialog">
      @if (podcast.type !== 'upload') {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Source URL</mat-label>
          <input
            matInput
            type="url"
            [ngModel]="urlInput()"
            (ngModelChange)="urlInput.set($event)"
            [disabled]="saving()"
          />
          <mat-hint>Changing the URL refreshes the cover, title and type.</mat-hint>
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
              <span>{{ findErr }} — keeping the current cover.</span>
            </div>
          } @else {
            @let detectedType = info?.type ?? podcast.type;
            <div class="preview">
              <img class="preview__cover" [src]="coverPreviewUrl()" [alt]="podcast.title" />
              <div class="preview__meta">
                <div class="preview__title">{{ info?.title ?? podcast.title }}</div>
                <div class="preview__sub">{{ detectedType }}</div>
              </div>
            </div>
          }
        </div>
      }

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Title</mat-label>
        <input
          matInput
          required
          [ngModel]="title()"
          (ngModelChange)="title.set($event)"
          [disabled]="saving()"
        />
        <mat-hint>Auto-filled when you change the source URL. Edit freely after.</mat-hint>
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Cover URL</mat-label>
        <input
          matInput
          type="url"
          [ngModel]="coverUrlInput()"
          (ngModelChange)="coverUrlInput.set($event)"
          [disabled]="saving()"
        />
        <mat-hint>Paste any image URL.</mat-hint>
      </mat-form-field>

      <ps-tags-field
        [tags]="tagsDraft()"
        (tagsChange)="tagsDraft.set($event)"
      />

      <mat-slide-toggle
        [checked]="hasToBeDeletedDraft()"
        (change)="hasToBeDeletedDraft.set($event.checked)"
      >
        Auto-delete old downloaded episodes
      </mat-slide-toggle>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button [disabled]="saving()" (click)="onCancel()">Cancel</button>
      <button
        mat-flat-button
        color="primary"
        [disabled]="saving() || !title().trim()"
        (click)="onSave()"
      >
        @if (saving()) {
          <mat-spinner diameter="18" />
        } @else {
          <mat-icon>save</mat-icon>
        }
        Save
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
    .preview--loading {
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
export class PodcastEditDialogComponent {
  private readonly podcastApi = inject(PodcastApi);
  private readonly findApi = inject(FindApi);
  private readonly snackbar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<PodcastEditDialogComponent, boolean>);
  protected readonly podcast = inject<PodcastHAL>(MAT_DIALOG_DATA);

  protected readonly urlInput = signal<string>(this.podcast.url ?? '');
  protected readonly title = signal<string>(this.podcast.title);
  protected readonly coverUrlInput = signal<string>(this.podcast.cover.url);
  protected readonly currentCover = signal<{ width: number; height: number; url: string }>({
    width: this.podcast.cover.width,
    height: this.podcast.cover.height,
    url: this.podcast.cover.url,
  });
  // Display via the proxy URL while the cover still points at the saved
  // source — once the user (or /find) supplies a different URL, that URL
  // has no proxy yet, so render it directly.
  protected readonly coverPreviewUrl = computed(() => {
    const cover = this.currentCover();
    return cover.url === this.podcast.cover.url ? this.podcast.cover.proxyURL : cover.url;
  });
  protected readonly tagsDraft = signal<readonly TagInput[]>(
    this.podcast.tags.map((t) => ({ id: t.id, name: t.name })),
  );
  protected readonly hasToBeDeletedDraft = signal<boolean>(this.podcast.hasToBeDeleted);

  protected readonly findInfo = signal<FindPodcastInformationHAL | undefined>(undefined);
  protected readonly findError = signal<string | undefined>(undefined);
  protected readonly finding = signal(false);
  private readonly updateMutation = this.podcastApi.updateMutation();
  protected readonly saving = this.updateMutation.isPending;

  private pendingFind: ReturnType<typeof setTimeout> | undefined;
  private pendingCoverDims: ReturnType<typeof setTimeout> | undefined;
  // Pre-seed with the current URL so opening the dialog doesn't auto-fire /find
  // — only an actual edit by the user does.
  private lastFoundUrl: string | undefined = (this.podcast.url ?? '').trim();

  constructor() {
    // Source URL → /find → refresh title + cover preview/draft.
    effect(() => {
      const url = this.urlInput().trim();
      if (this.pendingFind) {
        clearTimeout(this.pendingFind);
        this.pendingFind = undefined;
      }
      if (!isLikelyUrl(url) || url === this.lastFoundUrl) {
        return;
      }
      this.finding.set(true);
      this.findError.set(undefined);
      this.pendingFind = setTimeout(() => {
        this.findApi.byUrl(url).subscribe({
          next: (info) => {
            this.findInfo.set(info);
            this.findError.set(undefined);
            this.finding.set(false);
            this.lastFoundUrl = url;
            this.title.set(info.title);
            if (info.cover) {
              this.coverUrlInput.set(info.cover.url);
              this.currentCover.set({
                width: info.cover.width,
                height: info.cover.height,
                url: info.cover.url,
              });
            }
          },
          error: () => {
            this.findInfo.set(undefined);
            this.findError.set("Couldn't refresh metadata for this URL");
            this.finding.set(false);
          },
        });
      }, FIND_DEBOUNCE_MS);
    });

    // Cover URL field → infer width/height client-side via <img>, fallback to
    // 1000×1000 if the image refuses to load (CORS, 404, etc.). Skips when the
    // URL already matches the active cover (i.e. /find just synced it in).
    effect(() => {
      const url = this.coverUrlInput().trim();
      if (this.pendingCoverDims) {
        clearTimeout(this.pendingCoverDims);
        this.pendingCoverDims = undefined;
      }
      if (!isLikelyUrl(url) || url === this.currentCover().url) return;
      this.pendingCoverDims = setTimeout(() => {
        if (typeof Image === 'undefined') {
          this.currentCover.set({ width: 1000, height: 1000, url });
          return;
        }
        const img = new Image();
        img.onload = () =>
          this.currentCover.set({
            width: img.naturalWidth || 1000,
            height: img.naturalHeight || 1000,
            url,
          });
        img.onerror = () =>
          this.currentCover.set({ width: 1000, height: 1000, url });
        img.src = url;
      }, FIND_DEBOUNCE_MS);
    });
  }

  protected onCancel() {
    this.dialogRef.close(false);
  }

  protected onSave() {
    const cover = this.currentCover();
    const body: PodcastUpdateHAL = {
      id: this.podcast.id,
      title: this.title().trim(),
      url: this.urlInput().trim() || null,
      hasToBeDeleted: this.hasToBeDeletedDraft(),
      tags: this.tagsDraft().map((t) => (t.id ? { id: t.id, name: t.name } : { name: t.name })),
      cover: { width: cover.width, height: cover.height, url: cover.url },
    };
    this.updateMutation.mutate(
      { id: this.podcast.id, body },
      {
        onSuccess: () => {
          this.snackbar.open('Podcast saved', undefined, { duration: 2500 });
          this.dialogRef.close(true);
        },
        onError: () =>
          this.snackbar.open('Could not save the podcast', 'Dismiss', { duration: 4000 }),
      },
    );
  }
}

function isLikelyUrl(s: string): boolean {
  return /^https?:\/\/\S+\.\S+/.test(s);
}
