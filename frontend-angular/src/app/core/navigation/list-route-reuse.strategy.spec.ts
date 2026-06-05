import { ActivatedRouteSnapshot, DetachedRouteHandle } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';

import { ListRouteReuseStrategy } from './list-route-reuse.strategy';

function snapshot(
  configPath: string | null,
  url: string[] = [],
): ActivatedRouteSnapshot {
  return {
    routeConfig: configPath === null ? null : { path: configPath },
    url: url.map((path) => ({ path })),
  } as unknown as ActivatedRouteSnapshot;
}

function makeHandle() {
  const destroy = vi.fn();
  return {
    handle: { componentRef: { destroy } } as unknown as DetachedRouteHandle,
    destroy,
  };
}

describe('ListRouteReuseStrategy', () => {
  it('retains the spotlight page despite its falsy empty path', () => {
    const strategy = new ListRouteReuseStrategy();
    const spotlight = snapshot('');
    const { handle } = makeHandle();

    expect(strategy.shouldDetach(spotlight)).toBe(true);
    strategy.store(spotlight, handle);
    expect(strategy.shouldAttach(spotlight)).toBe(true);
    expect(strategy.retrieve(spotlight)).toBe(handle);
  });

  it('retains the list pages', () => {
    const strategy = new ListRouteReuseStrategy();
    for (const path of ['library', 'podcasts', 'playlists']) {
      expect(strategy.shouldDetach(snapshot(path))).toBe(true);
    }
  });

  it('does not retain unrelated routes', () => {
    const strategy = new ListRouteReuseStrategy();
    expect(strategy.shouldDetach(snapshot('settings'))).toBe(false);
    expect(
      strategy.shouldDetach(
        snapshot('podcasts/:idPodcast/items/:id', ['podcasts', 'A', 'items', 'i1']),
      ),
    ).toBe(false);
    expect(strategy.shouldDetach(snapshot(null))).toBe(false);
  });

  it('keys detail pages by resolved url so another podcast never reuses the DOM', () => {
    const strategy = new ListRouteReuseStrategy();
    const podcastA = snapshot('podcasts/:idPodcast', ['podcasts', 'A']);
    const podcastB = snapshot('podcasts/:idPodcast', ['podcasts', 'B']);
    const { handle } = makeHandle();

    expect(strategy.shouldDetach(podcastA)).toBe(true);
    strategy.store(podcastA, handle);

    expect(strategy.shouldAttach(podcastA)).toBe(true);
    expect(strategy.retrieve(podcastA)).toBe(handle);
    expect(strategy.shouldAttach(podcastB)).toBe(false);
    expect(strategy.retrieve(podcastB)).toBeNull();
  });

  it('keeps only the latest detail handle per route config and destroys the evicted one', () => {
    const strategy = new ListRouteReuseStrategy();
    const podcastA = snapshot('podcasts/:idPodcast', ['podcasts', 'A']);
    const podcastB = snapshot('podcasts/:idPodcast', ['podcasts', 'B']);
    const a = makeHandle();
    const b = makeHandle();

    strategy.store(podcastA, a.handle);
    strategy.store(podcastB, b.handle);

    expect(a.destroy).toHaveBeenCalled();
    expect(strategy.shouldAttach(podcastA)).toBe(false);
    expect(strategy.retrieve(podcastB)).toBe(b.handle);
  });

  it('does not evict across different detail configs', () => {
    const strategy = new ListRouteReuseStrategy();
    const podcast = snapshot('podcasts/:idPodcast', ['podcasts', 'A']);
    const playlist = snapshot('playlists/:idPlaylist', ['playlists', 'P']);
    const p = makeHandle();
    const l = makeHandle();

    strategy.store(podcast, p.handle);
    strategy.store(playlist, l.handle);

    expect(p.destroy).not.toHaveBeenCalled();
    expect(strategy.retrieve(podcast)).toBe(p.handle);
    expect(strategy.retrieve(playlist)).toBe(l.handle);
  });

  it('drops the stored handle when the router stores null after re-attach', () => {
    const strategy = new ListRouteReuseStrategy();
    const spotlight = snapshot('');
    const { handle } = makeHandle();

    strategy.store(spotlight, handle);
    strategy.store(spotlight, null);

    expect(strategy.shouldAttach(spotlight)).toBe(false);
    expect(strategy.retrieve(spotlight)).toBeNull();
  });
});
