import { defineConfig } from 'vitest/config';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  test: {
    environment: 'happy-dom',
    include: ['src/main/resources/static/js/**/*.spec.js'],
  },
  resolve: {
    alias: {
      'workforce-ui': join(__dirname, 'src/main/resources/static/js/workforce-ui.js'),
    },
  },
});
