import { Injectable, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';
export type SidenavMode = 'expanded' | 'rail';
export type EffectiveTheme = 'light' | 'dark';

const THEME_KEY = 'ps.theme';
const THEME_VALUES: readonly ThemePreference[] = ['system', 'light', 'dark'];

const SIDENAV_KEY = 'ps.sidenav';
const SIDENAV_VALUES: readonly SidenavMode[] = ['expanded', 'rail'];

@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly theme = signal<ThemePreference>(this.readInitialTheme());
  readonly sidenavMode = signal<SidenavMode>(this.readInitialSidenav());
  readonly effectiveTheme = signal<EffectiveTheme>(this.computeEffectiveTheme());

  constructor() {
    this.applyTheme(this.theme());
    this.applySidenav(this.sidenavMode());

    // Re-evaluate the effective theme when the OS preference changes while
    // the user is on 'system' (otherwise the explicit choice always wins).
    window
      .matchMedia('(prefers-color-scheme: dark)')
      .addEventListener('change', () => this.refreshEffectiveTheme());
  }

  setTheme(value: ThemePreference) {
    this.theme.set(value);
    this.writeKey(THEME_KEY, value);
    this.applyTheme(value);
    this.refreshEffectiveTheme();
  }

  setSidenavMode(value: SidenavMode) {
    this.sidenavMode.set(value);
    this.writeKey(SIDENAV_KEY, value);
    this.applySidenav(value);
  }

  toggleSidenavMode() {
    this.setSidenavMode(this.sidenavMode() === 'rail' ? 'expanded' : 'rail');
  }

  private readInitialTheme(): ThemePreference {
    const v = this.readKey(THEME_KEY);
    return v && (THEME_VALUES as readonly string[]).includes(v)
      ? (v as ThemePreference)
      : 'system';
  }

  private readInitialSidenav(): SidenavMode {
    const v = this.readKey(SIDENAV_KEY);
    return v && (SIDENAV_VALUES as readonly string[]).includes(v)
      ? (v as SidenavMode)
      : 'expanded';
  }

  private readKey(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      // localStorage can be disabled (private mode, sandboxed iframes); ignore.
      return null;
    }
  }

  private writeKey(key: string, value: string) {
    try {
      localStorage.setItem(key, value);
    } catch {
      // ignore
    }
  }

  private applyTheme(value: ThemePreference) {
    const root = document.documentElement;
    root.style.colorScheme = value === 'system' ? 'light dark' : value;
  }

  private applySidenav(value: SidenavMode) {
    document.documentElement.style.setProperty(
      '--sidenav-width',
      value === 'rail' ? '80px' : '240px',
    );
  }

  private computeEffectiveTheme(): EffectiveTheme {
    const t = this.theme();
    if (t === 'light' || t === 'dark') return t;
    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light';
  }

  private refreshEffectiveTheme() {
    this.effectiveTheme.set(this.computeEffectiveTheme());
  }
}
