/**
 * Created by kevin on 07/05/2016.
 */
import { Module, Component } from './../../../decorators';
import { TitleService } from './../../service/title.service';

@Module({ name : 'ps.common.component.title', modules : [ TitleService ] })
@Component({ selector : 'title', template : `{{ tc.title }}`, as: 'tc'})
export class TitleComponent {
    
    title = null;
    
    constructor(TitleService, $scope) {
        "ngInject";
        TitleService.title.subscribe(newTitle => $scope.$evalAsync(() => this.title = newTitle)); 
    }
} 