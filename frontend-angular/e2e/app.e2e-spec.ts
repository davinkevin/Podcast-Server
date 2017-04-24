import { PodcastServerPage } from './app.po';

describe('podcast-server App', () => {
  let page: PodcastServerPage;

  beforeEach(() => {
    page = new PodcastServerPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('ps works!');
  });
});
