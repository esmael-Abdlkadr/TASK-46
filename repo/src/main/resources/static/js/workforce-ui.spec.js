import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { attachConfirmDestructive } from './workforce-ui.js';

describe('attachConfirmDestructive', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        vi.stubGlobal('confirm', vi.fn(() => true));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('calls confirm with message from data-confirm', () => {
        document.body.innerHTML =
            '<button id="x" data-confirm="Really delete?">Del</button>';
        attachConfirmDestructive(document);

        document.getElementById('x').click();

        expect(globalThis.confirm).toHaveBeenCalledWith('Really delete?');
    });

    it('prevents default when user cancels confirm', () => {
        vi.stubGlobal('confirm', vi.fn(() => false));
        document.body.innerHTML =
            '<a id="x" href="/target" data-confirm="Sure?">Go</a>';
        attachConfirmDestructive(document);

        const ev = new MouseEvent('click', { bubbles: true, cancelable: true });
        const link = document.getElementById('x');
        const notCancelled = link.dispatchEvent(ev);

        expect(globalThis.confirm).toHaveBeenCalled();
        expect(notCancelled).toBe(false);
    });

    it('binds multiple elements independently', () => {
        document.body.innerHTML =
            '<button id="a" data-confirm="First?">A</button><button id="b" data-confirm="Second?">B</button>';
        attachConfirmDestructive(document);

        document.getElementById('a').click();
        document.getElementById('b').click();

        expect(globalThis.confirm).toHaveBeenNthCalledWith(1, 'First?');
        expect(globalThis.confirm).toHaveBeenNthCalledWith(2, 'Second?');
    });

    it('does nothing when data-confirm is absent', () => {
        document.body.innerHTML = '<button id="plain">Ok</button>';
        attachConfirmDestructive(document);

        document.getElementById('plain').click();

        expect(globalThis.confirm).not.toHaveBeenCalled();
    });
});
