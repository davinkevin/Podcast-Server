import { DebugElement } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import {
  MatButtonModule,
  MatCardModule,
  MatIconModule,
  MatInputModule,
  MatPaginatorModule,
  MatSelectModule,
  MatToolbarModule
} from '@angular/material';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Action, Store, StoreModule } from '@ngrx/store';
import { cold, hot } from 'jasmine-marbles';
import { Observable } from 'rxjs/Observable';
import { of } from 'rxjs/observable/of';

import { OpenSideNavAction } from '../app.actions';
import { Direction, Item, Page, Status } from '../shared/entity';
import { ItemService } from '../shared/service/item/item.service';
import { ToolbarModule } from '../shared/toolbar/toolbar.module';

import { Search, SearchActions, SearchSuccess } from './search.actions';
import { SearchComponent, StatusesViewValue } from './search.component';
import { SearchEffects } from './search.effects';
import * as fromSearch from './search.reducer';
import { searchRequest, searchResults } from './search.reducer';
import { TruncateModule } from 'ng2-truncate';
import { AppState } from '#app/app.reducer';
import { ComponentTester } from 'ngx-speculoos';

class SearchComponentTester extends ComponentTester<SearchComponent> {
  constructor() {
    super(SearchComponent);
  }

  get results() {
    return this.elements('.search__results mat-card');
  }

  get q() {
    return this.input('input[name="q"]');
  }

  get tags() {
    return this.input('input[name="tags"]');
  }

  get status() {
    return this.element('mat-select[name="status"]')
      .debugElement.componentInstance;
  }

  get property() {
    return this.element('mat-select[name="property"]')
      .debugElement.componentInstance;
  }

  get direction() {
    return this.element('mat-select[name="direction"]')
      .debugElement.componentInstance;
  }

  get topPaginator() {
    return this.elements('mat-paginator')[0];
  }
}


const searchResult = {
  content: [
    {
      id: '33c98205-51dd-4245-a86b-6c725121684d',
      cover: {
        id: 'c1faeff7-72e5-4c4e-b394-acbc6399a377',
        url: '/',
        width: 1280,
        height: 720
      },
      title: 'Net Neutrality Update:  Last Week Tonight with John Oliver (Web Exclusive)',
      url: 'https: //www.youtube.com/watch?v=qI5y-_sqJT0',
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-05-15T05: 01: 23.372+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/85366aef-5cfe-4716-9891-347d3f70150b/items/33c98205-51dd-4245-a86b-6c725121684d/download.mp4',
      podcastId: '85366aef-5cfe-4716-9891-347d3f70150b'
    },
    {
      id: 'fdf9d570-9386-483b-8d78-e8e0657c9feb',
      cover: {
        id: '22f2c6b6-1230-46c9-84be-9f3f6e2b2d03',
        url: '/',
        width: 200,
        height: 200
      },
      title: "RMC :  14/05 - L'Afterfoot - 23h-0h",
      url: 'http: //podcast.rmc.fr/channel59/20170514_afterfoot_9.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-15T01: 01: 38.141+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/8e99045e-c685-4757-9f93-d67d6d125332/items/fdf9d570-9386-483b-8d78-e8e0657c9feb/download.mp3',
      podcastId: '8e99045e-c685-4757-9f93-d67d6d125332'
    },
    {
      id: '124e8f13-29cb-4c96-9423-2988a4d335d4',
      cover: {
        id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 22h-23h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_2.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.921+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/124e8f13-29cb-4c96-9423-2988a4d335d4/download.mp3',
      podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
    },
    {
      id: '78cfdb14-31bc-4c8d-bb63-a2c1740aa2fb',
      cover: {
        id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 22h-23h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_2.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.61+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/78cfdb14-31bc-4c8d-bb63-a2c1740aa2fb/download.mp3',
      podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
    },
    {
      id: '4fd34d58-9f4d-417b-a2d1-f5e3a5b25219',
      cover: {
        id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 21h-22h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_1.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.92+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/4fd34d58-9f4d-417b-a2d1-f5e3a5b25219/download.mp3',
      podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
    },
    {
      id: 'f2ced3ce-13de-44b0-9ec2-faf8200c5282',
      cover: {
        id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 21h-22h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_1.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.61+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/f2ced3ce-13de-44b0-9ec2-faf8200c5282/download.mp3',
      podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
    },
    {
      id: '403ad0b2-da32-4c62-beba-c5fa955ac75a',
      cover: {
        id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 20h-21h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_0.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.609+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/403ad0b2-da32-4c62-beba-c5fa955ac75a/download.mp3',
      podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
    },
    {
      id: '667dcaa4-c945-4a04-baf8-a3b16924e9cb',
      cover: {
        id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'RMC :  14/05 - Intégrale Foot - 20h-21h',
      url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_0.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.916+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/667dcaa4-c945-4a04-baf8-a3b16924e9cb/download.mp3',
      podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
    },
    {
      id: '00b2310f-4a42-4959-a515-1e5b21defc4e',
      cover: { id: 'fc571904-0d60-4f40-b561-351e568acef0', url: '/', width: 600, height: 1 },
      title: 'Les épisodes de la semaine du 08 mai',
      url: 'http: //www.6play.fr/le-message-de-madenian-et-vdb-p_6730/les-episodes-de-la-semaine-du-08-mai-c_11686278',
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.68+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/624831fb-722c-49e6-ba44-256c194c8f66/items/00b2310f-4a42-4959-a515-1e5b21defc4e/download.mp4',
      podcastId: '624831fb-722c-49e6-ba44-256c194c8f66'
    },
    {
      id: '7b1d8095-6d0b-427f-a842-f74e0f935633',
      cover: {
        id: 'fa9f8213-6d44-405f-823d-cf9747007f0f',
        url: '/',
        width: 1400,
        height: 1400
      },
      title: 'WannaCry',
      url: 'https: //www.nolimitsecu.fr/wp-content/uploads/NoLimitSecu-WannaCry.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.595+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/102c6319-1b46-40ae-8cca-bc4fd8ba0789/items/7b1d8095-6d0b-427f-a842-f74e0f935633/download.mp3',
      podcastId: '102c6319-1b46-40ae-8cca-bc4fd8ba0789'
    },
    {
      id: 'ab3d517d-b484-4207-9300-726a4a872a3f',
      cover: {
        id: '9218eef8-c4e6-4d41-acf2-00cb7537f958',
        url: '/',
        width: 640,
        height: 480
      },
      title: "King's Quest Chapitre V - 01  - 'la part du gâteux' [4K60fps]",
      url: 'https: //www.youtube.com/watch?v=sohRCKdg1O0',
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.942+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/daf66f11-835c-48ac-9713-98b6d2a0cef3/items/ab3d517d-b484-4207-9300-726a4a872a3f/download.mp4',
      podcastId: 'daf66f11-835c-48ac-9713-98b6d2a0cef3'
    },
    {
      id: '01bc6166-a690-4372-9889-bbb69b83a87d',
      cover: {
        id: '866762c9-fd92-4277-bbb7-17c67b593ef5',
        url: '/',
        width: 200,
        height: 200
      },
      title: 'Yet Another Podcast #170 – Windows Template Studio',
      url: 'http: //jesseliberty.com/wp-content/media/Show170.mp3',
      mimeType: 'audio/mpeg',
      status: 'FINISH',
      creationDate: '2017-05-14T23: 33: 56.353+02: 00',
      isDownloaded: true,
      proxyURL: '/api/podcasts/040731fe-1c0f-45a0-8672-1bd85fdef0c4/items/01bc6166-a690-4372-9889-bbb69b83a87d/download.mp3',
      podcastId: '040731fe-1c0f-45a0-8672-1bd85fdef0c4'
    }
  ],
  totalPages: 259,
  totalElements: 3108,
  last: false,
  numberOfElements: 12,
  sort: [
    {
      direction: 'DESC',
      property: 'pubDate',
      ignoreCase: false,
      nullHandling: 'NATIVE',
      descending: true,
      ascending: false
    }
  ],
  first: true,
  size: 12,
  number: 0
};
const request = {
  q: '',
  page: 0,
  size: 12,
  status: [],
  tags: [],
  sort: [{ property: 'pubDate', direction: Direction.DESC }]
};

xdescribe('SearchFeature', () => {

  describe('"SearchEffects', () => {
    let effects: SearchEffects;
    let actions: Observable<Action>;
    let itemService: ItemService;

    const PAGE: Page<Item> = {
      content: [
        {
          id: '33c98205-51dd-4245-a86b-6c725121684d',
          cover: {
            id: 'c1faeff7-72e5-4c4e-b394-acbc6399a377',
            url: '/api/podcasts/85366aef-5cfe-4716-9891-347d3f70150b/items/33c98205-51dd-4245-a86b-6c725121684d/cover.jpg',
            width: 1280,
            height: 720
          },
          title: 'Net Neutrality Update:  Last Week Tonight with John Oliver (Web Exclusive)',
          url: 'https: //www.youtube.com/watch?v=qI5y-_sqJT0',
          mimeType: 'video/mp4',
          status: 'FINISH',
          creationDate: '2017-05-15T05: 01: 23.372+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/85366aef-5cfe-4716-9891-347d3f70150b/items/33c98205-51dd-4245-a86b-6c725121684d/download.mp4',
          podcastId: '85366aef-5cfe-4716-9891-347d3f70150b'
        },
        {
          id: 'fdf9d570-9386-483b-8d78-e8e0657c9feb',
          cover: {
            id: '22f2c6b6-1230-46c9-84be-9f3f6e2b2d03',
            url: '/api/podcasts/8e99045e-c685-4757-9f93-d67d6d125332/cover.jpg',
            width: 200,
            height: 200
          },
          title: "RMC :  14/05 - L'Afterfoot - 23h-0h",
          url: 'http: //podcast.rmc.fr/channel59/20170514_afterfoot_9.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-15T01: 01: 38.141+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/8e99045e-c685-4757-9f93-d67d6d125332/items/fdf9d570-9386-483b-8d78-e8e0657c9feb/download.mp3',
          podcastId: '8e99045e-c685-4757-9f93-d67d6d125332'
        },
        {
          id: '124e8f13-29cb-4c96-9423-2988a4d335d4',
          cover: {
            id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
            url: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 22h-23h',
          url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_2.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.921+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/124e8f13-29cb-4c96-9423-2988a4d335d4/download.mp3',
          podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
        },
        {
          id: '78cfdb14-31bc-4c8d-bb63-a2c1740aa2fb',
          cover: {
            id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
            url: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 22h-23h',
          url: 'http://podcast.rmc.fr/channel245/20170514_integrale_foot_2.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.61+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/78cfdb14-31bc-4c8d-bb63-a2c1740aa2fb/download.mp3',
          podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
        },
        {
          id: '4fd34d58-9f4d-417b-a2d1-f5e3a5b25219',
          cover: {
            id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
            url: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 21h-22h',
          url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_1.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.92+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/4fd34d58-9f4d-417b-a2d1-f5e3a5b25219/download.mp3',
          podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
        },
        {
          id: 'f2ced3ce-13de-44b0-9ec2-faf8200c5282',
          cover: {
            id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
            url: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 21h-22h',
          url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_1.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.61+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/f2ced3ce-13de-44b0-9ec2-faf8200c5282/download.mp3',
          podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
        },
        {
          id: '403ad0b2-da32-4c62-beba-c5fa955ac75a',
          cover: {
            id: '4f3cc31a-2991-40de-a9fe-b6f0f6e8da1f',
            url: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 20h-21h',
          url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_0.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.609+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/299ffb9c-45c5-4568-bc55-703befdc6562/items/403ad0b2-da32-4c62-beba-c5fa955ac75a/download.mp3',
          podcastId: '299ffb9c-45c5-4568-bc55-703befdc6562'
        },
        {
          id: '667dcaa4-c945-4a04-baf8-a3b16924e9cb',
          cover: {
            id: '111cf8d9-e1e3-40f3-84e4-35e93eba0120',
            url: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/cover.jpeg',
            width: 200,
            height: 200
          },
          title: 'RMC :  14/05 - Intégrale Foot - 20h-21h',
          url: 'http: //podcast.rmc.fr/channel245/20170514_integrale_foot_0.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.916+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/9c8e10ea-e61c-4985-9944-95697a6c0c97/items/667dcaa4-c945-4a04-baf8-a3b16924e9cb/download.mp3',
          podcastId: '9c8e10ea-e61c-4985-9944-95697a6c0c97'
        },
        {
          id: '00b2310f-4a42-4959-a515-1e5b21defc4e',
          cover: {
            id: 'fc571904-0d60-4f40-b561-351e568acef0',
            url: '/api/podcasts/624831fb-722c-49e6-ba44-256c194c8f66/items/00b2310f-4a42-4959-a515-1e5b21defc4e/cover.',
            width: 600,
            height: 1
          },
          title: 'Les épisodes de la semaine du 08 mai',
          url: 'http: //www.6play.fr/le-message-de-madenian-et-vdb-p_6730/les-episodes-de-la-semaine-du-08-mai-c_11686278',
          mimeType: 'video/mp4',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.68+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/624831fb-722c-49e6-ba44-256c194c8f66/items/00b2310f-4a42-4959-a515-1e5b21defc4e/download.mp4',
          podcastId: '624831fb-722c-49e6-ba44-256c194c8f66'
        },
        {
          id: '7b1d8095-6d0b-427f-a842-f74e0f935633',
          cover: {
            id: 'fa9f8213-6d44-405f-823d-cf9747007f0f',
            url: '/api/podcasts/102c6319-1b46-40ae-8cca-bc4fd8ba0789/cover.png',
            width: 1400,
            height: 1400
          },
          title: 'WannaCry',
          url: 'https: //www.nolimitsecu.fr/wp-content/uploads/NoLimitSecu-WannaCry.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.595+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/102c6319-1b46-40ae-8cca-bc4fd8ba0789/items/7b1d8095-6d0b-427f-a842-f74e0f935633/download.mp3',
          podcastId: '102c6319-1b46-40ae-8cca-bc4fd8ba0789'
        },
        {
          id: 'ab3d517d-b484-4207-9300-726a4a872a3f',
          cover: {
            id: '9218eef8-c4e6-4d41-acf2-00cb7537f958',
            url: '/api/podcasts/daf66f11-835c-48ac-9713-98b6d2a0cef3/items/ab3d517d-b484-4207-9300-726a4a872a3f/cover.jpg',
            width: 640,
            height: 480
          },
          title: "King's Quest Chapitre V - 01  - 'la part du gâteux' [4K60fps]",
          url: 'https: //www.youtube.com/watch?v=sohRCKdg1O0',
          mimeType: 'video/mp4',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.942+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/daf66f11-835c-48ac-9713-98b6d2a0cef3/items/ab3d517d-b484-4207-9300-726a4a872a3f/download.mp4',
          podcastId: 'daf66f11-835c-48ac-9713-98b6d2a0cef3'
        },
        {
          id: '01bc6166-a690-4372-9889-bbb69b83a87d',
          cover: {
            id: '866762c9-fd92-4277-bbb7-17c67b593ef5',
            url: '/api/podcasts/040731fe-1c0f-45a0-8672-1bd85fdef0c4/cover.png',
            width: 200,
            height: 200
          },
          title: 'Yet Another Podcast #170 – Windows Template Studio',
          url: 'http: //jesseliberty.com/wp-content/media/Show170.mp3',
          mimeType: 'audio/mpeg',
          status: 'FINISH',
          creationDate: '2017-05-14T23: 33: 56.353+02: 00',
          isDownloaded: true,
          proxyURL: '/api/podcasts/040731fe-1c0f-45a0-8672-1bd85fdef0c4/items/01bc6166-a690-4372-9889-bbb69b83a87d/download.mp3',
          podcastId: '040731fe-1c0f-45a0-8672-1bd85fdef0c4'
        }
      ],
      totalPages: 259,
      totalElements: 3108,
      last: false,
      numberOfElements: 12,
      sort: [
        {
          direction: 'DESC',
          property: 'pubDate',
          ignoreCase: false,
          nullHandling: 'NATIVE',
          descending: true,
          ascending: false
        }
      ],
      first: true,
      size: 12,
      number: 0
    };
    const request = {
      q: '',
      page: 0,
      size: 12,
      status: [],
      tags: [],
      sort: [{ property: 'pubDate', direction: Direction.DESC }]
    };

    beforeEach(() => {
      itemService = { search: jest.fn() };
      (itemService.search as jest.Mock<Page<Item>>).mockReturnValue(of(PAGE));
    });

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [SearchEffects, provideMockActions(() => actions), { provide: ItemService, useValue: itemService }]
      });

      effects = TestBed.get(SearchEffects);
    });

    describe('search$', () => {
      it(
        'should trigger search',
        async(() => {
          /* Given */
          actions = hot('--a-', { a: new Search(request) });

          const expected = cold('--b', { b: new SearchSuccess(PAGE) });

          expect(effects.search$).toBeObservable(expected);
          expect(itemService.search).toHaveBeenCalledWith(request);
        })
      );
    });
  });
});

fdescribe('SearchModule', () => {

  let tester: SearchComponentTester;
  let store: Store<AppState>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [SearchComponent],
      imports: [
        ReactiveFormsModule,
        RouterTestingModule,

        NoopAnimationsModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatInputModule,
        MatSelectModule,
        MatPaginatorModule,
        MatToolbarModule,

        ToolbarModule,

        TruncateModule,

        StoreModule.forRoot([]),
        StoreModule.forFeature('search', fromSearch.search)
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    store = TestBed.get(Store);
    spyOn(store, 'dispatch').and.callThrough();
  });

  beforeEach(async(() => {
    tester = new SearchComponentTester();
    tester.detectChanges();
  }));

  it('should create', async(() => {
    expect(tester.componentInstance).toBeTruthy();
  }));

  it('should display elements on this page', async(() => {
    /* Given */
    /* When  */
    store.dispatch(new SearchSuccess(searchResult));
    tester.detectChanges();
    /* Then  */
    expect(tester.results.length).toEqual(12);
  }));

  it('should have default value for search form', async(() => {
    /* Given */
    /* When  */
    store.dispatch(new Search(request));
    tester.detectChanges();
    /* Then  */
    expect(tester.q.value).toEqual('');
    expect(tester.tags.value).toEqual('');
    expect(tester.status.selected.value).toBe(StatusesViewValue.ALL);
    expect(tester.property.selected.value).toBe('pubDate')
    expect(tester.direction.selected.value).toBe(Direction.DESC);
  }));

  it('should be initialized with tags list', async(() => {
    /* Given */
    const tagRequest = {...request, tags: [{ name: 'Foo' }, { name: 'Bar' }]}
    /* When  */
    store.dispatch(new Search(tagRequest));
    tester.detectChanges();
    /* Then  */
    expect(tester.tags.value).toBe('Foo, Bar');
  }));

  it('should be initialized with FINISH status', async(() => {
    /* Given */
    const statusRequest = { ...request, status: [Status.FINISH]};
    /* When */
    store.dispatch(new Search(statusRequest));
    tester.detectChanges();
    /* Then */
    expect(tester.status.selected.value).toBe(StatusesViewValue.DOWNLOADED);
  }));

  it('should be initialized with NOT_DOWNLOADED status', async(() => {
    /* Given */
    const statusRequest = { ...request, status: [Status.NOT_DOWNLOADED]};
    /* When */
    store.dispatch(new Search(statusRequest));
    tester.detectChanges();
    /* Then */
    expect(tester.status.selected.value).toBe(StatusesViewValue.NOT_DOWNLOADED);
  }));

  it('should be initialized with ALL status', async(() => {
    /* Given */
    const statusRequest = { ...request, status: []};
    /* When */
    store.dispatch(new Search(statusRequest));
    tester.detectChanges();
    /* Then */
    expect(tester.status.selected.value).toBe(StatusesViewValue.ALL);
  }));

  it('should init pager', () => {
    /* Given */
    const topPaginator = tester.topPaginator;
    /* When  */
    console.log(topPaginator.debugElement);
    /* Then  */
  });

});
