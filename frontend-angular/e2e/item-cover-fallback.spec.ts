import { expect, test } from '@playwright/test';

// #287: once the nightly cleanup (#284) removes an item cover from the bucket,
// the item cover route redirects to the remote URL — often dead for an old
// item. The podcast cover is never cleaned up, so it stands in instead of a
// broken image.

const PODCAST_COVER = '/api/v1/podcasts/p1/cover.jpg';
const ITEM_COVER = '/api/v1/podcasts/p1/items/i-0/cover.png';

// 1×1 transparent PNG.
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=',
  'base64',
);

const PODCAST = {
  id: 'p1',
  title: 'A podcast',
  url: 'https://example.com/rss',
  hasToBeDeleted: true,
  lastUpdate: '2024-01-01T00:00:00Z',
  type: 'RSS',
  tags: [],
  cover: { id: 'c1', width: 200, height: 200, url: 'u', proxyURL: PODCAST_COVER },
};

const ITEM = {
  id: 'i-0',
  title: 'An old episode',
  url: 'u',
  pubDate: '2020-01-01T00:00:00Z',
  downloadDate: null,
  creationDate: '2020-01-01T00:00:00Z',
  description: '',
  mimeType: 'audio/mpeg',
  length: null,
  fileName: null,
  status: 'DELETED',
  podcast: { id: 'p1', title: 'A podcast', url: 'u' },
  cover: { id: 'c2', width: 200, height: 200, url: 'u', proxyURL: ITEM_COVER },
  isDownloaded: false,
  podcastId: 'p1',
  proxyURL: 'u',
};

test('an item whose cover is gone shows its podcast cover', async ({ page }) => {
  await page.route('**/api/v1/podcasts/p1', (route) => route.fulfill({ json: PODCAST }));
  await page.route('**/api/v1/podcasts/p1/stats/**', (route) => route.fulfill({ json: [] }));
  await page.route('**/api/v1/podcasts/p1/items*', (route) =>
    route.fulfill({
      json: {
        content: [ITEM],
        empty: false,
        first: true,
        last: true,
        number: 0,
        numberOfElements: 1,
        size: 24,
        totalElements: 1,
        totalPages: 1,
      },
    }),
  );
  await page.route(`**${ITEM_COVER}`, (route) => route.fulfill({ status: 404 }));
  await page.route(`**${PODCAST_COVER}`, (route) =>
    route.fulfill({ body: PNG, contentType: 'image/png' }),
  );

  await page.goto('/podcasts/p1');

  const cover = page.locator('ps-track-row img.row__cover');
  await expect(cover).toHaveAttribute('src', new RegExp(`${PODCAST_COVER}$`), { timeout: 30_000 });
  await expect.poll(() => cover.evaluate((img: HTMLImageElement) => img.naturalWidth)).toBe(1);
});
