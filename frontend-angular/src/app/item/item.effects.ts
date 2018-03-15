import {Injectable} from '@angular/core';
import {ItemService} from '../shared/service/item/item.service';
import {Actions, Effect, ofType} from '@ngrx/effects';
import {map, switchMap} from 'rxjs/operators';
import {Item} from '../shared/entity';
import {Observable} from 'rxjs/Observable';
import {Action} from '@ngrx/store';
import {FIND_ONE, FindOneAction, FindOneSuccessAction} from './item.actions';

@Injectable()
export class ItemEffects {

  @Effect()
  findOne$: Observable<Action> = this.actions$
    .pipe(
      ofType(FIND_ONE),
      switchMap((a: FindOneAction) => this.itemService.findById(a.itemId, a.podcastId)),
      map((i: Item) => new FindOneSuccessAction(i))
    );

  constructor(private actions$: Actions, private itemService: ItemService) {}
}
