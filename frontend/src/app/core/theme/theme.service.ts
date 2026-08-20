import { Injectable, signal } from '@angular/core';

export type AppearancePreference = 'SYSTEM' | 'LIGHT' | 'DARK';
export type ThemeTone = 'teal' | 'blue' | 'amber';

export interface ThemeConfig {
  brandPrimary?: string;
  brandSecondary?: string;
  surface?: string;
  surfaceElevated?: string;
  textPrimary?: string;
  textMuted?: string;
  border?: string;
  focus?: string;
  success?: string;
  warning?: string;
  danger?: string;
  info?: string;
  appearance?: AppearancePreference;
  curatedThemeId?: ThemeTone;
}

export interface UserUiPreferences {
  appearance?: AppearancePreference;
  curatedThemeId?: ThemeTone;
}

export interface ResolvedUiTheme {
  brandPrimary: string;
  brandSecondary: string;
  surface: string;
  surfaceElevated: string;
  textPrimary: string;
  textMuted: string;
  border: string;
  focus: string;
  success: string;
  warning: string;
  danger: string;
  info: string;
  appearance: 'LIGHT' | 'DARK';
}

const DEFAULT_THEME: ResolvedUiTheme = {
  brandPrimary: '#006b5f', brandSecondary: '#0b6e9e', surface: '#f3f6f8',
  surfaceElevated: '#ffffff', textPrimary: '#17242b', textMuted: '#5d6b72',
  border: '#cbd7dc', focus: '#0b6e9e', success: '#16734a', warning: '#9a5b00',
  danger: '#b42318', info: '#1769aa', appearance: 'LIGHT',
};

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<ResolvedUiTheme>(DEFAULT_THEME);

  resolve(organization: ThemeConfig = {}, preferences: UserUiPreferences = {}): void {
    const safeOrganization = this.safeTheme(organization);
    const appearance = this.resolveAppearance(safeOrganization.appearance ?? preferences.appearance);
    const resolved = { ...DEFAULT_THEME, ...safeOrganization, appearance } as ResolvedUiTheme;
    this.theme.set(resolved);
    this.apply(resolved);
  }

  private safeTheme(theme: ThemeConfig): Partial<ResolvedUiTheme> {
    const result: Partial<ResolvedUiTheme> = {};
    for (const key of ['brandPrimary', 'brandSecondary', 'surface', 'surfaceElevated', 'textPrimary', 'textMuted', 'border', 'focus', 'success', 'warning', 'danger', 'info'] as const) {
      if (!theme[key] || this.isSafeColor(theme[key])) result[key] = theme[key];
    }
    return result;
  }

  private isSafeColor(value: string): boolean {
    return /^(#[0-9a-f]{3,8}|rgb(a)?\([\d\s,.%]+\)|hsl(a)?\([\d\s,.%]+\))$/i.test(value);
  }

  private resolveAppearance(preference?: AppearancePreference): 'LIGHT' | 'DARK' {
    if (preference === 'DARK') return 'DARK';
    if (preference === 'LIGHT') return 'LIGHT';
    return typeof matchMedia === 'function' && matchMedia('(prefers-color-scheme: dark)').matches ? 'DARK' : 'LIGHT';
  }

  private apply(theme: ResolvedUiTheme): void {
    const root = document.documentElement;
    const variables: Record<string, string> = {
      '--av-color-brand': theme.brandPrimary, '--av-color-brand-strong': theme.brandSecondary,
      '--av-color-canvas': theme.surface, '--av-color-surface': theme.surfaceElevated,
      '--av-color-ink': theme.textPrimary, '--av-color-muted': theme.textMuted,
      '--av-color-border': theme.border, '--av-color-focus': theme.focus,
      '--av-color-success': theme.success, '--av-color-warning': theme.warning,
      '--av-color-danger': theme.danger, '--av-color-info': theme.info,
    };
    for (const [name, value] of Object.entries(variables)) root.style.setProperty(name, value);
    root.dataset['appearance'] = theme.appearance.toLowerCase();
  }
}