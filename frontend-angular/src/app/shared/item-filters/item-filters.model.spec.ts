import { describe, expect, it } from 'vitest';

import { ITEM_STATUSES, ItemStatus } from '../../core/models/item.model';
import { STATUS_FILTER_OPTIONS, isStatusFilter, statusesFor } from './item-filters.model';

describe('statusesFor', () => {
  it('sends no status at all for "all"', () => {
    expect(statusesFor('all')).toEqual([]);
  });

  it('sends only FINISH for "downloaded"', () => {
    expect(statusesFor('downloaded')).toEqual(['FINISH']);
  });

  it('sends every status except FINISH for "not downloaded"', () => {
    // Asserted twice on purpose: against the derivation, so a status added to
    // ITEM_STATUSES joins the bucket automatically, and against an explicit
    // list, so such a change is *visible* in a diff rather than silent.
    expect(statusesFor('not-downloaded')).toEqual(ITEM_STATUSES.filter((s) => s !== 'FINISH'));
    expect(statusesFor('not-downloaded')).toEqual([
      'NOT_DOWNLOADED',
      'STARTED',
      'PAUSED',
      'DELETED',
      'STOPPED',
      'FAILED',
    ]);
  });

  it('keeps DELETED in the "not downloaded" bucket', () => {
    // A retention purge sets DELETED and nulls FILE_NAME, so the file is gone
    // from disk — it belongs with the not-downloaded ones, as it did in v1.
    expect(statusesFor('not-downloaded')).toContain('DELETED');
    expect(statusesFor('downloaded')).not.toContain('DELETED');
  });

  it('puts every known status in exactly one bucket', () => {
    // The property that matters: a status added to the backend cannot fall
    // outside both options without failing here.
    const downloaded = statusesFor('downloaded');
    const notDownloaded = statusesFor('not-downloaded');

    for (const status of ITEM_STATUSES) {
      const buckets = [downloaded, notDownloaded].filter((b) => b.includes(status));
      expect(buckets, `status ${status} must be in exactly one bucket`).toHaveLength(1);
    }
  });
});

describe('STATUS_FILTER_OPTIONS', () => {
  it('offers exactly three choices, never the raw enum', () => {
    // Regression guard against someone re-exposing the six/seven backend
    // statuses as individual checkboxes.
    expect(STATUS_FILTER_OPTIONS.map((o) => o.value)).toEqual([
      'all',
      'downloaded',
      'not-downloaded',
    ]);
    expect(STATUS_FILTER_OPTIONS.map((o) => o.label)).toEqual([
      'All',
      'Downloaded',
      'Not downloaded',
    ]);
  });
});

describe('isStatusFilter', () => {
  it('accepts the three known values', () => {
    expect(isStatusFilter('all')).toBe(true);
    expect(isStatusFilter('downloaded')).toBe(true);
    expect(isStatusFilter('not-downloaded')).toBe(true);
  });

  it('rejects anything else, including a raw backend status', () => {
    expect(isStatusFilter(undefined)).toBe(false);
    expect(isStatusFilter('')).toBe(false);
    expect(isStatusFilter('FINISH' as unknown as ItemStatus)).toBe(false);
    expect(isStatusFilter('not_downloaded')).toBe(false);
  });
});
