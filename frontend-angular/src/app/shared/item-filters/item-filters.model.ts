import { ITEM_STATUSES, ItemStatus } from '../../core/models/item.model';

/**
 * What the status control offers: a single choice between three, never the raw
 * backend enum. This mirrors the v1 UI, whose `<select>` offered exactly
 * All / Downloaded / Not downloaded — see
 * `frontend-angularjs/www/app/search/search.js:166-176`.
 */
export type StatusFilter = 'all' | 'downloaded' | 'not-downloaded';

export interface StatusFilterOption {
  readonly value: StatusFilter;
  readonly label: string;
}

export const STATUS_FILTER_OPTIONS: readonly StatusFilterOption[] = [
  { value: 'all', label: 'All' },
  { value: 'downloaded', label: 'Downloaded' },
  { value: 'not-downloaded', label: 'Not downloaded' },
];

/** The single status that means "the file is on disk". */
const DOWNLOADED_STATUS: ItemStatus = 'FINISH';

/**
 * Expands a filter choice into the statuses to send to the API.
 *
 * "Not downloaded" is *derived* — every status except `FINISH` — rather than
 * listed. A status added to the backend then joins the right bucket on its own
 * instead of silently falling outside both options, which is exactly the bug
 * v1's hardcoded array shipped with (it never listed `PAUSED`).
 *
 * `all` returns an empty list, and `ItemApi.search` omits empty lists, so it
 * sends no `status` parameter at all.
 */
export function statusesFor(filter: StatusFilter): readonly ItemStatus[] {
  switch (filter) {
    case 'all':
      return [];
    case 'downloaded':
      return [DOWNLOADED_STATUS];
    case 'not-downloaded':
      return ITEM_STATUSES.filter((s) => s !== DOWNLOADED_STATUS);
  }
}

/** Guards a `?status=` query param, which can hold anything a user typed. */
export function isStatusFilter(value: string | undefined): value is StatusFilter {
  return STATUS_FILTER_OPTIONS.some((o) => o.value === value);
}
