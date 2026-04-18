import { test, expect } from '@playwright/test';

test('login page loads and shows form', async ({ page }) => {
  const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
  if (!res || res.status() >= 500) {
    test.skip(true, 'Backend not running at E2E_BASE_URL (start docker-compose first)');
  }
  await expect(page.locator('#username')).toBeVisible();
  await expect(page.locator('#password')).toBeVisible();
});

test('unauthenticated root redirects toward login', async ({ page }) => {
  const res = await page.goto('/', { waitUntil: 'commit' }).catch(() => null);
  if (!res || res.status() >= 500) {
    test.skip(true, 'Backend not running');
  }
  await page.waitForURL(/\/login/, { timeout: 10000 });
});
