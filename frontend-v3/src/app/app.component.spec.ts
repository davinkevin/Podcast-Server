import { describe, expect, it } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('renders the topbar brand inside the shell', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([]), provideNoopAnimations()],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const brand: HTMLElement = fixture.nativeElement.querySelector('.topbar__title');
    expect(brand?.textContent).toContain('Podcast Server');
  });
});
