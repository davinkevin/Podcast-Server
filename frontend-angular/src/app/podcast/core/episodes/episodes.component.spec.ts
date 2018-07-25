import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EpisodesComponent } from './episodes.component';
import { MatListModule, MatPaginatorModule } from '@angular/material';
import * as fromPodcast from '../../podcast.reducer';
import { RouterTestingModule } from '@angular/router/testing';
import { Store, StoreModule } from '@ngrx/store';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs/observable/of';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DatePipe } from '@angular/common';
import { PodcastState } from '#app/podcast/podcast.reducer';
import { FindItemsByPodcastsAndPageSuccessAction } from '#app/podcast/podcast.actions';

const items = {
  content: [
    {
      id: '02452898-ed6e-45ab-90b7-c923586a1f9a',
      cover: {
        id: 'd1e091f0-5c70-4cca-a596-b026d4460df7',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/02452898-ed6e-45ab-90b7-c923586a1f9a/cover.jpg',
        width: 1280,
        height: 720
      },
      title: "Panfried Cod and Ratatouille | Bart's Fish Tales",
      url: 'https://www.youtube.com/watch?v=B44mNHaMhv0',
      pubDate: '2018-01-07T13:30:00+01:00',
      description:
        "The Brilliant Bart is back and this time he is in Nusfjord in Norway to cook up some super fresh, sustainably sourced cod. It might be snowing but this dish is sure to warm you up. Served with a simple Ratatouille this flavour packed dish will really hit the spot.  \n\nTo catch more of Bart’s incredible recipes check out his channel | http://jamieol.com/BartsFishTales\n\nLinks from the video:\nFresh Seafood Linguine | http://jamieol.com/SeafoodLinguine\nScampi & Chips | http://jamieol.com/FTScampi\nFresh Poached Cod with Buttered Veg | http://jamieol.com/PoachedCodandVeg\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2018-01-07T14:04:06.191+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/02452898-ed6e-45ab-90b7-c923586a1f9a/Panfried_Cod_and_Ratatouille___Bart_s_Fish_Tales.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: '1dda2396-169d-48e8-ac36-70f0e1f94e02',
      cover: {
        id: 'c19150b7-54f3-49de-99ef-3cbf26450c7f',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/1dda2396-169d-48e8-ac36-70f0e1f94e02/cover.jpg',
        width: 1280,
        height: 720
      },
      title: 'Friday Night Feast | Four Awesome Weekend Dishes | Episodes 1-4',
      url: 'https://www.youtube.com/watch?v=UiZU0NbSGIw',
      pubDate: '2018-01-05T20:30:00+01:00',
      description:
        "Jamie’s been cooking up some brilliant dishes that are perfect for the weekend, so here are four of them for you to enjoy that we’re sure you’re gonna love:\nChicken Caesar Salad, Roasted Meat Pie, Provençal Bake & Moroccan M’hanncha\nIf you’re in the UK, check out the full show on All4 - http://channel4.com/now\n\nLinks from the video:\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2018-01-05T21:03:24.566+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/1dda2396-169d-48e8-ac36-70f0e1f94e02/Friday_Night_Feast___Four_Awesome_Weekend_Dishes___Episodes_1-4.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: 'b19f1152-4169-4c15-a7ce-8aa4e26d7a0d',
      cover: {
        id: '831049a7-027d-4461-ad37-1743bf02974c',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/b19f1152-4169-4c15-a7ce-8aa4e26d7a0d/cover.jpg',
        width: 640,
        height: 480
      },
      title: 'Kick off a Healthy 2018 | 1 hour Mega Mix',
      url: 'https://www.youtube.com/watch?v=NWkL_R_r6aw',
      pubDate: '2018-01-02T14:48:35+01:00',
      description:
        'The best Healthy recipes from the archives all laid back to back in an hour long mega mix!! Perfect to watch while pounding the miles on the treadmill to work up an appetite!',
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2018-01-02T15:02:11.076+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/b19f1152-4169-4c15-a7ce-8aa4e26d7a0d/Kick_off_a_Healthy_2018___1_hour_Mega_Mix.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: 'da58df18-4aaa-46d1-8a7f-70f6f7187b31',
      cover: {
        id: '99118b06-fd35-4259-b5f8-5ebefa9fba93',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/da58df18-4aaa-46d1-8a7f-70f6f7187b31/cover.jpg',
        width: 1280,
        height: 720
      },
      title: 'The Perfect Bacon Sandwich | Jamie Oliver | HNY',
      url: 'https://www.youtube.com/watch?v=ljnis-8YjoY',
      pubDate: '2017-12-31T13:30:02+01:00',
      description:
        "There’s no better breakfast treat than a bacon sandwich but what is the secret to making the perfect one? Jamie and his mate Pete go head to head and give us their tips for making that bacon sarnie just that little bit more special. \n\nThis indulgent treat is one of 100 recipes from Jamie’s Comfort Food book. Get your copy here http://jamieol.com/ComfortFoodBook\n\nComfort Food originally aired on Channel 4. If you’re UK based you can watch the whole programme on All 4 http://www.channel4.com/now\n\nLinks from the video:\nCrispy Duck Lasagne | http://jamieol.com/CrispyDuckLasagne\nGrilling a Steak with my Incredible Kitchen Car | http://jamieol.com/GrillingSteak\nAntipasti Meat Plank | http://jamieol.com/meatplank\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-31T14:02:30.538+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/da58df18-4aaa-46d1-8a7f-70f6f7187b31/The_Perfect_Bacon_Sandwich___Jamie_Oliver___HNY.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: '7d071fde-b696-4c78-b99d-0a58e43c30b7',
      cover: {
        id: 'ba699230-2eff-4ca1-8e81-ab9de1f7e19a',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/7d071fde-b696-4c78-b99d-0a58e43c30b7/cover.jpg',
        width: 1280,
        height: 720
      },
      title: "HAPPY CHRISTMAS | Behind the Scenes of Jamie's Christmas Competition.",
      url: 'https://www.youtube.com/watch?v=GoN35b7vkWM',
      pubDate: '2017-12-25T12:00:01+01:00',
      description:
        'Secretly Jamie has always wanted to be a Game Show host and this year he finally got the opportunity to live out his fantasy!! \n\nN.B. No Directors were harmed during the making of the film! \n\nHAPPY CHRISTMAS ONE AND ALL!!',
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-25T12:01:50.05+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/7d071fde-b696-4c78-b99d-0a58e43c30b7/HAPPY_CHRISTMAS___Behind_the_Scenes_of_Jamie_s_Christmas_Competition..mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: '75915ccc-c06d-49e7-b765-4976e29c52fd',
      cover: {
        id: '1421aa17-10ee-4a2d-81d5-e82808713001',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/75915ccc-c06d-49e7-b765-4976e29c52fd/cover.jpg',
        width: 1280,
        height: 720
      },
      title: 'Perfect Mince Pies | Jamie Oliver',
      url: 'https://www.youtube.com/watch?v=gVqPt2sdfkU',
      pubDate: '2017-12-24T13:30:00+01:00',
      description:
        "Christmas isn’t Christmas without a few mince pies floating around and this recipe ticks all the boxes for a quick, tasty treat full of festive flavour without the buttery pastry casing. Roast chestnuts, sour fruits and sweet mincemeat are encased in puff pastry and baked in filo cups until golden and bubbling. Finish off your pies with a sprinkling of icing sugar and pile high on a plate for a fantastic tale centre piece. Enjoy! This recipe first aired in the UK in 2008 on Channel 4 as part of the Jamie At Home Christmas Special.\n\nLinks from the video:\nSuper Simple Chocolate and Pears  | http://jamieol.com/PearsandChocolate\nJamie's EPIC Christmas Giveaway | http://jamieol.com/ChristmasGiveaway\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-24T14:01:49.387+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/75915ccc-c06d-49e7-b765-4976e29c52fd/Perfect_Mince_Pies___Jamie__Oliver.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: '9fe38f42-a9ae-4233-baa2-a4a2d7fbdaa1',
      cover: {
        id: '7c44e1a4-5051-464d-877e-fba9e6dedd2c',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/9fe38f42-a9ae-4233-baa2-a4a2d7fbdaa1/cover.jpg',
        width: 1280,
        height: 720
      },
      title: "Jamie Oliver's Christmas Classics Mega Mix. X",
      url: 'https://www.youtube.com/watch?v=cknAs00z-5I',
      pubDate: '2017-12-23T23:55:42+01:00',
      description:
        "A one stop shop for all of the best Christmas recipes from Jamie. Sit back and enjoy over an hour of Jamie's best bits.",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-24T01:01:41.322+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/9fe38f42-a9ae-4233-baa2-a4a2d7fbdaa1/Jamie_Oliver_s_Christmas_Classics_Mega_Mix.__X.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: 'b980250c-a722-4f36-a830-697fcdeaf199',
      cover: {
        id: '93e84a8a-6524-4035-90ea-ea8386884998',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/b980250c-a722-4f36-a830-697fcdeaf199/cover.jpg',
        width: 1280,
        height: 720
      },
      title: 'The Perfect Steak | Barbecoa Piccadilly',
      url: 'https://www.youtube.com/watch?v=eS36H360ahk',
      pubDate: '2017-12-23T13:13:19+01:00',
      description:
        "Jamie loves the steak at Barbecoa Piccadilly - it's worth all the effort the team go to in sourcing the meat, taking the time to dry age it, and then cooking it with incredible smoke and fire. Have a look at this and then book to come visit! Don't forget to check out the other videos Jamie has done too.\n\nBook a table | http://jamieoliver.com/barbecoa/meat\nMore Barbecoa Videos | http://jamieol.com/BarbecoaPlaylist\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-23T14:02:01.771+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/b980250c-a722-4f36-a830-697fcdeaf199/The_Perfect_Steak___Barbecoa_Piccadilly.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: '8dfdc6e6-f34e-442d-bb56-da93c2f2301d',
      cover: {
        id: '2ecdb756-5b15-43d7-a9b3-54e0248ff636',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/8dfdc6e6-f34e-442d-bb56-da93c2f2301d/cover.jpg',
        width: 1280,
        height: 720
      },
      title: "Jamie's Italian Christmas | Cracker Ravioli, Balsamic Potatoes, Porchetta and Tiramisu | Channel 4",
      url: 'https://www.youtube.com/watch?v=gxOjccDk8Kg',
      pubDate: '2017-12-20T20:30:00+01:00',
      description:
        "Jamie's Italian Christmas TV show is packed full of delicious dishes for this festive period, and here we have four of Jamie's favourites for you to enjoy: \nIncredible Cracker Ravioli with Squash, Sage and Ricotta\nBeautiful Balsamic Potatoes for a twist on the classic Roastie\nMind-blowing Italian Porchetta with delicious stuffing\nChristmas Tiramisu with Clementine\n\nTake a look and give them a whirl, or check out the recipes in Jamie's Christmas Cookbook - \nhttp://jamieol.com/christmasbook\n\nIf you are UK based you can watch the show on All4: http://www.channel4.com/now\n\nLinks from the video:\nClassic Christmas Tips & Hacks | http://jamieol.com/ChristmasTips\nSuper Simple Chocolate and Pears | http://jamieol.com/PearsandChocolate\nChristmas Ham & Eggs | http://jamieol.com/ChristmasHamEggs\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-20T21:04:42.961+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/8dfdc6e6-f34e-442d-bb56-da93c2f2301d/Jamie_s_Italian_Christmas___Cracker_Ravioli__Balsamic_Potatoes__Porchetta_and_Tiramisu___Channel_4.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    },
    {
      id: 'fd59a909-8a0b-41f0-aa95-738f8d4cd299',
      cover: {
        id: '0335f9d7-627f-4940-81ff-37eba6dc0def',
        url: '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/fd59a909-8a0b-41f0-aa95-738f8d4cd299/cover.jpg',
        width: 1280,
        height: 720
      },
      title: 'Christmas Gingerbread Men | Jools Oliver',
      url: 'https://www.youtube.com/watch?v=lCGr6XAw0gk',
      pubDate: '2017-12-19T20:30:01+01:00',
      description:
        "Have some fun in the kitchen this with a classic gingerbread recipe your kids will love to make. Decorate your people using dried fruits, nuts and coconut for a healthier treat that tastes fantastic. A great way to spend time with the kids and have something delicious to eat as well!\n\nLinks from the video:\nSuper Simple Chocolate and Pears  | http://jamieol.com/PearsandChocolate\nJamie's EPIC Christmas Giveaway | http://jamieol.com/ChristmasGiveaway\n\nBook your Christmas party now at Jamie’s Italian http://jamieol.com/JIChristmas\n\nFor more information on any Jamie Oliver products featured on the channel click here: \nhttp://www.jamieoliver.com/shop/homeware/\n\nFor more nutrition info, click here: http://jamieol.com/Nutrition\n\nSubscribe to Food Tube | http://jamieol.com/FoodTube\nSubscribe to Drinks Tube | http://jamieol.com/DrinksTube\nSubscribe to Family Food Tube | http://jamieol.com/FamilyFoodTube\nTwitter | http://jamieol.com/FTTwitter\nInstagram |http://jamieol.com/FTInstagram\nFacebook | http://jamieol.com/FTFacebook\nMore great recipes | http://www.jamieoliver.com\nJamie's Recipes App | http://jamieol.com/JamieApp\n\n#FOODTUBE\n\nx",
      mimeType: 'video/mp4',
      status: 'FINISH',
      creationDate: '2017-12-19T21:03:58.315+01:00',
      proxyURL:
        '/api/podcasts/94b65e37-24ad-4297-b7bc-e4324a6dc0ed/items/fd59a909-8a0b-41f0-aa95-738f8d4cd299/Christmas_Gingerbread_Men___Jools_Oliver.mp4',
      isDownloaded: true,
      podcastId: '94b65e37-24ad-4297-b7bc-e4324a6dc0ed'
    }
  ],
  totalPages: 150,
  totalElements: 1498,
  last: false,
  numberOfElements: 10,
  first: true,
  sort: [{ direction: 'DESC', property: 'pubDate', ignoreCase: false, nullHandling: 'NATIVE', descending: true, ascending: false }],
  size: 10,
  number: 0
};

describe('EpisodesComponent', () => {
	let component: EpisodesComponent;
	let fixture: ComponentFixture<EpisodesComponent>;
	let el: DebugElement;
  let store: Store<PodcastState>;
	let datePipe: DatePipe;

	beforeEach(
		async(() => {
			TestBed.configureTestingModule({
				declarations: [EpisodesComponent],
				imports: [
					MatListModule,
          MatPaginatorModule,
          RouterTestingModule.withRoutes([]),

					StoreModule.forRoot({}),
					StoreModule.forFeature('podcast', fromPodcast.reducer),

					RouterTestingModule
				],
			}).compileComponents();

			datePipe = new DatePipe('en-US');
		})
	);

  beforeEach(async () => {
    store = TestBed.get(Store);
    spyOn(store, 'dispatch').and.callThrough();
  });

	beforeEach(async () => {
	  store.dispatch(new FindItemsByPodcastsAndPageSuccessAction(items));
		fixture = TestBed.createComponent(EpisodesComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
		el = fixture.debugElement;
		await fixture.whenStable();
	});

	it('should create', () => {
	  /* Given */
    /* When */
    /* Then */
		expect(component).toBeTruthy();
	});

	it('should have coherent number of items', () => {
		/* Given */
		/* When  */
		const itemsElement = el.queryAll(By.css('[mat-list-item]'));
		/* Then  */
		expect(itemsElement.length).toEqual(10);
	});

	it('should have each line with cover', () => {
		/* Given */
		const coversExpected = items.content.map(v => v.cover.url);
		/* When  */
		const coversUrl = el.queryAll(By.css('img')).map(v => v.properties.src);
		/* Then  */
		expect(coversExpected).toEqual(coversUrl);
	});

	it('should have each line with title', () => {
		/* Given */
		const titleExpected = items.content.map(v => v.title);
		/* When  */
		const titles = el.queryAll(By.css('h3[matLine]')).map(asText);
		/* Then  */
		expect(titleExpected).toEqual(titles);
	});

	it('should have each line with date', () => {
		/* Given */
		const dateExpected = items.content.map(v => v.pubDate).map(v => datePipe.transform(v, 'dd/MM/yyyy à HH:mm'));
		/* When  */
		const dates = el.queryAll(By.css('p[matLine]')).map(asText);
		/* Then  */
		expect(dateExpected).toEqual(dates);
	});

	function asText(v: DebugElement) {
		return v.nativeElement.textContent.trim();
	}
});
