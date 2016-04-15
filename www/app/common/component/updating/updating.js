/**
 * Created by kevin on 25/10/2015 for Podcast Server
 */
import angular from 'angular';
import {Component, View, Module} from '../../../decorators';
import AngularStompDKConfig from '../../../config/ngstomp';
import template from './updating.html!text';
import './updating.css!';

@Module({
    name : 'ps.common.component.updating',
    modules : [ AngularStompDKConfig ]
})
@Component({
    selector : 'update-status',
    as : 'uc'
})
@View({
    template : template
})
export default class UpdatingStatusComponent {

    constructor(ngstomp, $scope) {
        "ngInject";
        this.ngstomp = ngstomp;
        this.isUpdating = false;
        this.$scope = $scope;

        this.ngstomp
                .subscribeTo('/app/updating')
                .callback((message) => this.updateStatus(message))
                .withBodyInJson()
                .bindTo($scope)
            .and()
                .subscribeTo('/topic/updating')
                .callback((message) => this.updateStatus(message))
                .withBodyInJson()
                .bindTo($scope)
            .connect();
    }

    updateStatus(message) {
        this.isUpdating = message.body;
    }

    static link(scope, element) {

        let liParent = element.parent().parent()[0];

        if (liParent && liParent.tagName === 'LI') {
            let liElement = angular.element(liParent);
            liElement.addClass('hidden');
            scope.$watch(
                'uc.isUpdating',
                (newValue) => (newValue) ? liElement.removeClass('hidden') : liElement.addClass('hidden')
            );
        }
    }
}
