import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ItemComponent } from './item.component';

xdescribe('ItemComponent', () => {
	let component: ItemComponent;
	let fixture: ComponentFixture<ItemComponent>;

	beforeEach(
		async(() => {
			TestBed.configureTestingModule({ declarations: [ItemComponent] }).compileComponents();
		})
	);

	beforeEach(() => {
		fixture = TestBed.createComponent(ItemComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
