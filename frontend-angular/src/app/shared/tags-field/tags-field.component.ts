import { COMMA, ENTER } from '@angular/cdk/keycodes';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipInput, MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import {
  MatAutocomplete,
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
      <mat-label>{{ label() }}</mat-label>
      <mat-chip-grid #grid>
        @for (tag of tags(); track tag.name; let i = $index) {
          <mat-chip-row (removed)="onRemove(i)">
            {{ tag.name }}
            <button matChipRemove [attr.aria-label]="'Remove ' + tag.name">
              <mat-icon>cancel</mat-icon>
            </button>
          </mat-chip-row>
        }
        <!-- Tag names are not prose: no browser autocomplete, no autocorrect,
             no auto-capitalisation. The matAutocomplete directive happens to set
             autocomplete="off" today; stating it here means the behaviour does
             not rest on that side effect. The only suggestions shown are our
             own, from TagApi. -->
        <input
          [matChipInputFor]="grid"
          [matAutocomplete]="auto"
          [matChipInputSeparatorKeyCodes]="separatorKeys"
          [matChipInputAddOnBlur]="!suggestionsOpen()"
          [placeholder]="placeholder()"
          autocomplete="off"
          autocorrect="off"
          autocapitalize="none"
          spellcheck="false"
          [(ngModel)]="query"
          (matChipInputTokenEnd)="onChipInputEnd($event)"
          (keydown.tab)="onTabComplete($event)"
        />
      </mat-chip-grid>
      <mat-autocomplete
        #auto="matAutocomplete"
        (optionSelected)="onSelect($event)"
        (opened)="suggestionsOpen.set(true)"
        (closed)="suggestionsOpen.set(false)"
      >
        @if (noMatch()) {
          <!-- Says so, rather than showing an empty panel and letting the tag be
               typed in anyway: in lookup mode there is nothing to commit. -->
          <mat-option disabled>No tag matches</mat-option>
        }
        @for (suggestion of suggestions(); track suggestion.id) {
          <mat-option [value]="suggestion">{{ suggestion.name }}</mat-option>
        }
      </mat-autocomplete>
      <mat-hint>{{ hint() }}</mat-hint>
    </mat-form-field>
  `,
  styles: `
    :host {
      display: block;
    }
    .tags {
      width: 100%;
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagsFieldComponent {
  readonly tags = model<readonly TagInput[]>([]);

  /** Defaults keep the podcast edit dialog — the original caller — unchanged. */
  readonly label = input<string>('Tags');
  readonly hint = input<string>('Press Enter or comma to add a tag.');
  readonly placeholder = input<string>('');

  /**
   * Whether typing a name that no tag carries creates one.
   *
   * True where that is the point — the podcast edit dialog, whose whole job is
   * attaching new tags. False where it can only mislead: as a *filter*, a name
   * no tag carries matches nothing, so committing free text hands back an empty
   * result set for what looks like a typo, or merely the wrong case.
   */
  readonly allowNew = input(true);

  private readonly auto = viewChild.required(MatAutocomplete);
  private readonly chipInput = viewChild.required(MatChipInput);

  protected readonly separatorKeys = [ENTER, COMMA];
  protected readonly query = signal<string>('');

  /**
   * Add-on-blur is right when the field loses focus to elsewhere — what was
   * typed should be kept. It is wrong while suggestions are showing, because
   * clicking one blurs the input on its way, and that blur used to commit the
   * typed prefix as a tag of its own alongside the suggestion picked.
   */
  protected readonly suggestionsOpen = signal(false);

  private readonly tagApi = inject(TagApi);
  private readonly searchInput = computed<TagSearchInput | undefined>(() => {
    const q = this.query().trim();
    return q.length > 0 ? { name: q } : undefined;
  });
  private readonly suggestionsQuery = this.tagApi.search(this.searchInput);

  protected readonly suggestions = computed(() => {
    const result = this.suggestionsQuery.data();
    if (!result) return [];
    const taken = new Set(this.tags().map((t) => t.name.toLowerCase()));
    return result.content.filter((s) => !taken.has(s.name.toLowerCase()));
  });

  /** A searched-for name that matched nothing — only meaningful in lookup mode. */
  protected readonly noMatch = computed(
    () =>
      !this.allowNew() &&
      this.query().trim().length > 0 &&
      this.suggestionsQuery.data() !== undefined &&
      this.suggestions().length === 0,
  );

  protected onChipInputEnd(event: MatChipInputEvent) {
    // Enter is both a chip separator and the autocomplete's "take the
    // highlighted suggestion" key, and both handlers see the keystroke. When a
    // suggestion is highlighted the autocomplete owns it: letting the separator
    // through as well added the typed prefix *and* the picked suggestion, so
    // arrowing onto "dev" after typing "d" produced two tags, "d" and "dev".
    if (this.autocompleteHasHighlight()) return;

    const value = event.value.trim();
    // In lookup mode the only way in is picking a suggestion. Clear what was
    // typed rather than leave it dangling beside the committed chips, where it
    // would read as a filter that is not actually in force.
    if (!this.allowNew()) {
      event.chipInput?.clear();
      this.query.set('');
      return;
    }

    if (value && !this.tags().some((t) => t.name.toLowerCase() === value.toLowerCase())) {
      this.tags.update((prev) => [...prev, { name: value }]);
    }
    event.chipInput?.clear();
    this.query.set('');
  }

  private autocompleteHasHighlight(): boolean {
    const auto = this.auto();
    return auto.isOpen && auto.options.some((option) => option.active);
  }

  /**
   * Takes the highlighted suggestion — or the first one — on Tab, so a lookup
   * costs one key instead of ArrowDown then Enter.
   *
   * Only in lookup mode. Where new tags are allowed, Tab already does something
   * meaningful with what was typed, and completing instead would silently swap
   * a name being created for an existing one that merely shares its prefix.
   *
   * Tab is consumed only when there is something to take, so focus is never
   * trapped: taking a suggestion empties the query, which closes the panel, and
   * the next Tab moves on as usual. Shift+Tab always means "go back".
   */
  protected onTabComplete(event: Event) {
    if (this.allowNew() || (event as KeyboardEvent).shiftKey) return;

    // Not gated on the panel being open: Material's autocomplete trigger closes
    // it on Tab before this listener runs. What matters is only that something
    // was typed and something matches it.
    if (this.query().trim().length === 0) return;

    const highlighted = this.auto().options.find((o) => o.active && !o.disabled)?.value as
      TagInput | undefined;
    const target = highlighted ?? this.suggestions()[0];
    if (!target) return;

    event.preventDefault();
    this.add(target);
  }

  protected onSelect(event: MatAutocompleteSelectedEvent) {
    this.add(event.option.value as TagInput);
  }

  private add(tag: TagInput) {
    if (!this.tags().some((t) => t.name.toLowerCase() === tag.name.toLowerCase())) {
      this.tags.update((prev) => [...prev, { id: tag.id, name: tag.name }]);
    }
    // Clearing the text control is the separator path's job, and taking a
    // suggestion skips it — so do it here too, or the typed prefix stays behind
    // in the field next to the tag it just produced. The autocomplete also
    // writes the picked option back into the control, so resetting the model
    // alone is not enough.
    this.chipInput().clear();
    this.query.set('');
  }

  protected onRemove(index: number) {
    this.tags.update((prev) => prev.filter((_, i) => i !== index));
  }
}
