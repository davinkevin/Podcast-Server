import { FrontendNg2Page } from './app.po';

describe('frontend-ng2 App', function() {
  let page: FrontendNg2Page;

  beforeEach(() => {
    page = new FrontendNg2Page();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
