import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FloatingPlayerComponent } from './floating-player.component';
import { MatIconModule, MatToolbarModule } from '@angular/material';
import { Store, StoreModule } from '@ngrx/store';
import { floatingPlayer } from '#app/floating-player/floating-player.reducer';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { AppState } from '#app/app.reducer';
import { Item } from '#app/shared/entity';
import { PlayAction } from '#app/floating-player/floating-player.actions';

const VIDEO_ITEM: Item = {
	id: '94b33c39-7f90-47f5-af80-30448defd49b',
	cover: {
		id: '06363f42-f51a-4f05-a4ac-20b032273647',
		url: '/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/cover.jpg',
		width: 1280,
		height: 720
	},
	title: 'Grilled Cheese Nachos - Handle It',
	url: 'https://www.youtube.com/watch?v=xJFc4Ajwn84',
	pubDate: '2018-03-24T05:00:04+01:00',
	description:
		"Let Ameer teach you how to make Grilled Cheese Nachos! Great for yourself and a small group of friends!\n\nIngredients\n1 Package of bacon\n14 Slices of white bread\n7 Slices of cheddar cheese \n1 Cup of melted butter \n1 Cup of shredded cheese \n1/3 Cup of sliced jalepenos \n1/2 Cup tomato soup\n1/2 Cup of salsa\n\nTools\n1 Chef knife \n1 Cutting board \n2 Frying pans \n1 Wooden spoon \n1 Spatula \n1 Cooking brush\n1 Oven tray \nParchment paper \n1 Small sauce pan \n1 Laddle\n\nStep 1\nCut bacon into bits and cook in a frying pan on medium heat for 10 minutes. \n\nStep 2 \nPut a slice of cheese between two pieces of white bread then grill sandwich in a buttered frying pan. Make about 5 sandwiches then cut into shapes of torillia chips.\n\nStep 3\nLine an oven tray with parchment paper. Spread out grilled cheese chips and garnish with shredded cheese, jalepenos and bacon bits then bake in the oven at 400 degrees Fahrenheit until cheese is melted.\n\nStep 4\nIn a small sauce pan warm tomato soup with salsa on low heat.\n\nStep 5 \nAdd the final toppings tomato soup salsa, avocado slices and sour cream stream.\n\nCheck out Harley's Video Diaries - http://www.youtube.com/HarleyMore\n\nWe have a #YouTubeGaming Channel:\nhttp://www.YouTube.com/OriginalGamerShow\n\nFollow the guys!\n@harleyplays @chefatari @epicgrossguy @cooldan @itsmikesantos\n\nLIKE/FAVORITE and SHARE for new meals every week!\n\nGrilled Cheese Nachos  - Handle It",
	mimeType: 'video/mp4',
	length: 67125489,
	fileName: 'Grilled_Cheese_Nachos_-_Handle_It.mp4',
	status: 'FINISH',
	progression: 0,
	downloadDate: '2018-03-24T06:03:37.202+01:00',
	creationDate: '2018-03-24T06:01:33.559+01:00',
	podcastId: 'b806d042-0ced-40b6-89bf-1401cad98bd2',
	proxyURL:
		'/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/Grilled_Cheese_Nachos_-_Handle_It.mp4',
	isDownloaded: true
};
const AUDIO_ITEM: Item = {
	id: '94b33c39-7f90-47f5-af80-30448defd49b',
	cover: {
		id: '06363f42-f51a-4f05-a4ac-20b032273647',
		url: '/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/cover.jpg',
		width: 1280,
		height: 720
	},
	title: 'Grilled Cheese Nachos - Handle It',
	url: 'https://www.youtube.com/watch?v=xJFc4Ajwn84',
	pubDate: '2018-03-24T05:00:04+01:00',
	description:
		"Let Ameer teach you how to make Grilled Cheese Nachos! Great for yourself and a small group of friends!\n\nIngredients\n1 Package of bacon\n14 Slices of white bread\n7 Slices of cheddar cheese \n1 Cup of melted butter \n1 Cup of shredded cheese \n1/3 Cup of sliced jalepenos \n1/2 Cup tomato soup\n1/2 Cup of salsa\n\nTools\n1 Chef knife \n1 Cutting board \n2 Frying pans \n1 Wooden spoon \n1 Spatula \n1 Cooking brush\n1 Oven tray \nParchment paper \n1 Small sauce pan \n1 Laddle\n\nStep 1\nCut bacon into bits and cook in a frying pan on medium heat for 10 minutes. \n\nStep 2 \nPut a slice of cheese between two pieces of white bread then grill sandwich in a buttered frying pan. Make about 5 sandwiches then cut into shapes of torillia chips.\n\nStep 3\nLine an oven tray with parchment paper. Spread out grilled cheese chips and garnish with shredded cheese, jalepenos and bacon bits then bake in the oven at 400 degrees Fahrenheit until cheese is melted.\n\nStep 4\nIn a small sauce pan warm tomato soup with salsa on low heat.\n\nStep 5 \nAdd the final toppings tomato soup salsa, avocado slices and sour cream stream.\n\nCheck out Harley's Video Diaries - http://www.youtube.com/HarleyMore\n\nWe have a #YouTubeGaming Channel:\nhttp://www.YouTube.com/OriginalGamerShow\n\nFollow the guys!\n@harleyplays @chefatari @epicgrossguy @cooldan @itsmikesantos\n\nLIKE/FAVORITE and SHARE for new meals every week!\n\nGrilled Cheese Nachos  - Handle It",
	mimeType: 'audio/mp3',
	length: 67125489,
	fileName: 'Grilled_Cheese_Nachos_-_Handle_It.mp3',
	status: 'FINISH',
	progression: 0,
	downloadDate: '2018-03-24T06:03:37.202+01:00',
	creationDate: '2018-03-24T06:01:33.559+01:00',
	podcastId: 'b806d042-0ced-40b6-89bf-1401cad98bd2',
	proxyURL:
		'/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/Grilled_Cheese_Nachos_-_Handle_It.mp4',
	isDownloaded: true
};
const UNKNOW_ITEM: Item = {
	id: '94b33c39-7f90-47f5-af80-30448defd49b',
	cover: {
		id: '06363f42-f51a-4f05-a4ac-20b032273647',
		url: '/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/cover.jpg',
		width: 1280,
		height: 720
	},
	title: 'Grilled Cheese Nachos - Handle It',
	url: 'https://www.youtube.com/watch?v=xJFc4Ajwn84',
	pubDate: '2018-03-24T05:00:04+01:00',
	description:
		"Let Ameer teach you how to make Grilled Cheese Nachos! Great for yourself and a small group of friends!\n\nIngredients\n1 Package of bacon\n14 Slices of white bread\n7 Slices of cheddar cheese \n1 Cup of melted butter \n1 Cup of shredded cheese \n1/3 Cup of sliced jalepenos \n1/2 Cup tomato soup\n1/2 Cup of salsa\n\nTools\n1 Chef knife \n1 Cutting board \n2 Frying pans \n1 Wooden spoon \n1 Spatula \n1 Cooking brush\n1 Oven tray \nParchment paper \n1 Small sauce pan \n1 Laddle\n\nStep 1\nCut bacon into bits and cook in a frying pan on medium heat for 10 minutes. \n\nStep 2 \nPut a slice of cheese between two pieces of white bread then grill sandwich in a buttered frying pan. Make about 5 sandwiches then cut into shapes of torillia chips.\n\nStep 3\nLine an oven tray with parchment paper. Spread out grilled cheese chips and garnish with shredded cheese, jalepenos and bacon bits then bake in the oven at 400 degrees Fahrenheit until cheese is melted.\n\nStep 4\nIn a small sauce pan warm tomato soup with salsa on low heat.\n\nStep 5 \nAdd the final toppings tomato soup salsa, avocado slices and sour cream stream.\n\nCheck out Harley's Video Diaries - http://www.youtube.com/HarleyMore\n\nWe have a #YouTubeGaming Channel:\nhttp://www.YouTube.com/OriginalGamerShow\n\nFollow the guys!\n@harleyplays @chefatari @epicgrossguy @cooldan @itsmikesantos\n\nLIKE/FAVORITE and SHARE for new meals every week!\n\nGrilled Cheese Nachos  - Handle It",
	mimeType: null,
	length: 67125489,
	fileName: 'Grilled_Cheese_Nachos_-_Handle_It.mp3',
	status: 'FINISH',
	progression: 0,
	downloadDate: '2018-03-24T06:03:37.202+01:00',
	creationDate: '2018-03-24T06:01:33.559+01:00',
	podcastId: 'b806d042-0ced-40b6-89bf-1401cad98bd2',
	proxyURL:
		'/api/podcasts/b806d042-0ced-40b6-89bf-1401cad98bd2/items/94b33c39-7f90-47f5-af80-30448defd49b/Grilled_Cheese_Nachos_-_Handle_It.mp4',
	isDownloaded: true
};

describe('FloatingPlayerComponent', () => {
	let comp: FloatingPlayerComponent;
	let f: ComponentFixture<FloatingPlayerComponent>;
	let el: DebugElement;
	let store: Store<AppState>;

	beforeEach(
		async(() => {
			TestBed.configureTestingModule({
				declarations: [FloatingPlayerComponent],
				imports: [MatIconModule, MatToolbarModule, StoreModule.forRoot({}), StoreModule.forFeature('floatingPlayer', floatingPlayer)]
			}).compileComponents();
		})
	);

	beforeEach(async () => {
		f = TestBed.createComponent(FloatingPlayerComponent);
		comp = f.componentInstance;
		el = f.debugElement;
		f.detectChanges();
		await f.whenStable();
	});

	beforeEach(() => {
		store = TestBed.get(Store);
		jest.spyOn(store, 'dispatch');
	});

	it('should create', () => {
		expect(comp).toBeTruthy();
	});

	it('should not be displayed if no item', () => {
		/* Given */
		/* When  */
		const player = el.query(By.css('.player'));
		/* Then  */
		expect(player).toBeNull();
	});

	describe('with a video item', () => {
		beforeEach(async () => {
			store.dispatch(new PlayAction(VIDEO_ITEM));
			f.detectChanges();
			await f.whenStable();
		});

		it('should show player if item defined', async () => {
			const player = el.query(By.css('.player'));
			expect(player).not.toBeNull();
		});

		it('should close when click on close icon', async () => {
			/* Given */
			const closeIcon = el.query(By.css('mat-icon'));

			/* When  */
			closeIcon.nativeElement.click();
			f.detectChanges();
			await f.whenStable();

			/* Then  */
			const player = el.query(By.css('.player'));
			expect(player).toBeNull();
		});

		it('should display a video player', () => {
			const videoPlayer = el.query(By.css('video'));
			expect(videoPlayer).not.toBeNull();
		});
	});

	describe('with an audio item', () => {
		beforeEach(async () => {
			store.dispatch(new PlayAction(AUDIO_ITEM));
			f.detectChanges();
			await f.whenStable();
		});

		it('should display a video player', () => {
			const audioPlayer = el.query(By.css('audio'));
			expect(audioPlayer).not.toBeNull();
		});

		it('should close when click on close icon', async () => {
			/* Given */
			const closeIcon = el.query(By.css('mat-icon'));

			/* When  */
			closeIcon.nativeElement.click();
			f.detectChanges();
			await f.whenStable();

			/* Then  */
			const player = el.query(By.css('.player'));
			expect(player).toBeNull();
		});
	});

	describe('with an unknown item', () => {
		beforeEach(async () => {
			store.dispatch(new PlayAction(UNKNOW_ITEM));
			f.detectChanges();
			await f.whenStable();
		});

		it('should display a video player', () => {
			const audioPlayer = el.query(By.css('audio'));
			const videoPlayer = el.query(By.css('video'));

			expect(audioPlayer).toBeNull();
			expect(videoPlayer).toBeNull();
		});
	});
});
