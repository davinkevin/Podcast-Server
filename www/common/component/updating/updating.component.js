/**
 * Created by kevin on 25/10/2015.
 */

import './updating.css!';
import template from './updating.html!text';

export default class UpdatingController {

    constructor(ngstomp, $scope) {
        "ngInject";
        this.ngstomp = ngstomp;
        this.isUpdating = false;
        this.$scope = $scope;

        this.ngstomp
            .subscribe('/app/updating', (message) => this.updateStatus(message), {}, $scope)
            .subscribe('/topic/updating', (message) => this.updateStatus(message), {}, $scope);
    }

    updateStatus(message) {
        this.isUpdating = JSON.parse(message.body);
    }
    
    static component() {
        return {
            restrict : 'E',
            scope : true,
            template : template,
            controller : 'UpdatingController',
            controllerAs : 'uc',
            link : (scope, element) => {

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
    }
}
