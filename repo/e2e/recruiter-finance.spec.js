import { test, expect } from '@playwright/test';

test('recruiter reaches dashboard then unified search', async ({ page }) => {
    const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
    if (!res || res.status() >= 500) {
        test.skip(true, 'Backend not running');
    }

    await page.fill('#username', 'recruiter');
    await page.fill('#password', 'recruiter123');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/recruiter\/dashboard/, { timeout: 20000 });

    await page.goto('/search/unified', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveURL(/\/search\/unified/);
    await expect(page.getByRole('heading', { name: 'Unified Search' })).toBeVisible();
});

test('finance reaches payments area', async ({ page }) => {
    const res = await page.goto('/login', { waitUntil: 'domcontentloaded' }).catch(() => null);
    if (!res || res.status() >= 500) {
        test.skip(true, 'Backend not running');
    }

    await page.fill('#username', 'finance');
    await page.fill('#password', 'finance123');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/finance\/dashboard/, { timeout: 20000 });

    await page.goto('/finance/payments', { waitUntil: 'domcontentloaded' });
    await expect(page).toHaveURL(/\/finance\/payments/);
    await expect(page.getByRole('heading', { name: 'Payment Transactions' })).toBeVisible();
});
