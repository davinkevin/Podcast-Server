import { browser, element, by } from 'protractor';

export class PodcastServerPage {
  navigateTo() {
    return browser.get('/');
  }

  getParagraphText() {
    return element(by.css('ps-root h1')).getText();
  }
}
