import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { initApp } from './app.js';

describe('initApp', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        vi.stubGlobal('confirm', vi.fn(() => true));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('wires data-confirm handlers using attachConfirmDestructive behavior', () => {
        document.body.innerHTML =
            '<button id="del" type="button" data-confirm="Really?">X</button>';
        initApp(document);

        document.getElementById('del').click();

        expect(globalThis.confirm).toHaveBeenCalledWith('Really?');
    });
});
