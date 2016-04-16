/**
 * Created by kevin on 25/10/2015 for Podcast Server
 */
import angular from 'angular';
import {Component, Module} from '../../../decorators';
import AngularStompDKConfig from '../../../config/ngstomp';
import template from './updating.html!text';
import './updating.css!';

@Module({
    name : 'ps.common.component.updating',
    modules : [ AngularStompDKConfig ]
})
@Component({
    selector : 'update-status',
    as : 'uc',
    template : template
})
export default class UpdatingStatusComponent {

    isUpdating = false;

    constructor(ngstomp, $scope, $element) {
        "ngInject";
        this.ngstomp = ngstomp;
        this.$scope = $scope;
        this.$element = $element;
    }

    $onInit(){
        this.ngstomp
            .subscribeTo('/app/updating')
                .callback(message => this.updateStatus(message))
                .withBodyInJson().bindTo(this.$scope)
            .and()
            .subscribeTo('/topic/updating')
                .callback(message => this.updateStatus(message))
                .withBodyInJson().bindTo(this.$scope)
            .connect();

        let liParent = this.$element.parent().parent()[0];

        if (liParent && liParent.tagName === 'LI') {
            let liElement = angular.element(liParent);
            liElement.addClass('hidden');
            this.$scope.$watch( () => this.isUpdating,
                newValue => (newValue) ? liElement.removeClass('hidden') : liElement.addClass('hidden')
            );
        }
    }

    updateStatus(message) {
        this.isUpdating = message.body;
    }
}
