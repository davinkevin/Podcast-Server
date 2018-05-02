import { Action } from '@ngrx/store';
import { Item } from '#app/shared/entity';

export enum FloatingPlayerAction {
	PLAY = '[FLOATING_PLAYER] Play',
	CLOSE = '[FLOATING_PLAYER] Close'
}

export class PlayAction implements Action {
	readonly type = FloatingPlayerAction.PLAY;
	constructor(public item: Item) {}
}
export class CloseAction implements Action {
	readonly type = FloatingPlayerAction.CLOSE;
}

export type FloatingPlayerActions = PlayAction | CloseAction;
