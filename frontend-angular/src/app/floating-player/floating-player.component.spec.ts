import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FloatingPlayerComponent } from './floating-player.component';

xdescribe('FloatingPlayerComponent', () => {
	let component: FloatingPlayerComponent;
	let fixture: ComponentFixture<FloatingPlayerComponent>;

	beforeEach(
		async(() => {
			TestBed.configureTestingModule({
				declarations: [FloatingPlayerComponent]
			}).compileComponents();
		})
	);

	beforeEach(() => {
		fixture = TestBed.createComponent(FloatingPlayerComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
