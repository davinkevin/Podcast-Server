import { COMMA, ENTER } from '@angular/cdk/keycodes';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  model,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import {
  MatChipInputEvent,
  MatChipsModule,
} from '@angular/material/chips';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
} from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

import { TagApi, TagSearchInput } from '../../core/api/tag.api';
import { TagInput } from '../../core/models/tag.model';

@Component({
  selector: 'ps-tags-field',
  standalone: true,
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatChipsModule,
    MatAutocompleteModule,
    MatInputModule,
    MatIconModule,
  ],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="tags">
      <mat-label>Tags</mat-label>
      <mat-chip-grid #grid>
        @for (tag of tags(); track tag.name; let i = $index) {
          <mat-chip-row (removed)="onRemove(i)">
            {{ tag.name }}
            <button matChipRemove [attr.aria-label]="'Remove ' + tag.name">
              <mat-icon>cancel</mat-icon>
            </button>
          </mat-chip-row>
        }
        <input
          [matChipInputFor]="grid"
          [matAutocomplete]="auto"
          [matChipInputSeparatorKeyCodes]="separatorKeys"
          [matChipInputAddOnBlur]="true"
          [(ngModel)]="query"
          (matChipInputTokenEnd)="onChipInputEnd($event)"
        />
      </mat-chip-grid>
      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="onSelect($event)">
        @for (suggestion of suggestions(); track suggestion.id) {
          <mat-option [value]="suggestion">{{ suggestion.name }}</mat-option>
        }
      </mat-autocomplete>
      <mat-hint>Press Enter or comma to add a tag.</mat-hint>
    </mat-form-field>
  `,
  styles: `
    :host { display: block; }
    .tags { width: 100%; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagsFieldComponent {
  readonly tags = model<readonly TagInput[]>([]);

  protected readonly separatorKeys = [ENTER, COMMA];
  protected readonly query = signal<string>('');

  private readonly tagApi = inject(TagApi);
  private readonly searchInput = computed<TagSearchInput | undefined>(() => {
    const q = this.query().trim();
    return q.length > 0 ? { name: q } : undefined;
  });
  private readonly suggestionsResource = this.tagApi.search(this.searchInput);

  protected readonly suggestions = computed(() => {
    const result = this.suggestionsResource.value();
    if (!result) return [];
    const taken = new Set(this.tags().map((t) => t.name.toLowerCase()));
    return result.content.filter((s) => !taken.has(s.name.toLowerCase()));
  });

  protected onChipInputEnd(event: MatChipInputEvent) {
    const value = event.value.trim();
    if (value && !this.tags().some((t) => t.name.toLowerCase() === value.toLowerCase())) {
      this.tags.update((prev) => [...prev, { name: value }]);
    }
    event.chipInput?.clear();
    this.query.set('');
  }

  protected onSelect(event: MatAutocompleteSelectedEvent) {
    const tag = event.option.value as TagInput;
    if (!this.tags().some((t) => t.name.toLowerCase() === tag.name.toLowerCase())) {
      this.tags.update((prev) => [...prev, { id: tag.id, name: tag.name }]);
    }
    this.query.set('');
  }

  protected onRemove(index: number) {
    this.tags.update((prev) => prev.filter((_, i) => i !== index));
  }
}
