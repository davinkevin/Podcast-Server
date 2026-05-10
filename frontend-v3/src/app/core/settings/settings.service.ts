import { Injectable, signal } from '@angular/core';

export type ThemePreference = 'system' | 'light' | 'dark';

const THEME_KEY = 'ps.theme';
const THEME_VALUES: readonly ThemePreference[] = ['system', 'light', 'dark'];

@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly theme = signal<ThemePreference>(this.readInitial());

  constructor() {
    this.apply(this.theme());
  }

  setTheme(value: ThemePreference) {
    this.theme.set(value);
    try {
      localStorage.setItem(THEME_KEY, value);
    } catch {
      // localStorage can be disabled (private mode, sandboxed iframes); ignore.
    }
    this.apply(value);
  }

  private readInitial(): ThemePreference {
    try {
      const v = localStorage.getItem(THEME_KEY);
      if (v && (THEME_VALUES as readonly string[]).includes(v)) {
        return v as ThemePreference;
      }
    } catch {
      // ignore
    }
    return 'system';
  }

  private apply(value: ThemePreference) {
    const root = document.documentElement;
    root.style.colorScheme =
      value === 'system' ? 'light dark' : value;
  }
}
