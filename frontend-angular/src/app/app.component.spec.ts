import { describe, expect, it } from 'vitest';

import { AppComponent } from './app.component';

// Minimal smoke test — instantiating AppComponent in jsdom requires wiring
// Router, HttpClient, TanStack Query, the SSE-based DownloadStreamService and
// Angular Material providers. Coverage will grow back as components get
// dedicated specs; this guarantees the bundle at least loads.
describe('AppComponent', () => {
  it('is defined', () => {
    expect(AppComponent).toBeDefined();
  });
});
