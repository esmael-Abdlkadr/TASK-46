import { attachConfirmDestructive } from './workforce-ui.js';

export function initApp(root) {
    attachConfirmDestructive(root || document);
}

document.addEventListener('DOMContentLoaded', function () {
    initApp(document);
});
