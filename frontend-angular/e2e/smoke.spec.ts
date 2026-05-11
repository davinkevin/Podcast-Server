import { expect, test } from '@playwright/test';

test('the v3 shell loads and redirects to /library', async ({ page }) => {
  await page.goto('/v3/');

  await expect(page).toHaveTitle(/Library — Podcast Server/);
  await expect(page).toHaveURL(/\/v3\/library/);
  await expect(page.getByRole('heading', { name: 'Library' })).toBeVisible();
});
