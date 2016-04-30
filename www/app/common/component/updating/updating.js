/**
 * Created by kevin on 25/10/2015 for Podcast Server
 */
import angular from 'angular';
import {Component, Module} from '../../../decorators';
import DownloadManager from '../../../common/service/data/downloadManager';
import template from './updating.html!text';
import './updating.css!';

@Module({
    name : 'ps.common.component.updating',
    modules : [ DownloadManager ]
})
@Component({
    selector : 'update-status',
    as : 'uc',
    template : template
})
export default class UpdatingStatusComponent {

    isUpdating = false;

    constructor(DonwloadManager, $scope, $element) {
        "ngInject";
        this.DownloadManager = DonwloadManager;
        this.$scope = $scope;
        this.$element = $element;
    }

    $onInit(){
        this.isUpdatingSub = this.DownloadManager
            .updating$
            .subscribe(isUpdating => this.$scope.$evalAsync(() => this.isUpdating = isUpdating));

        let liParent = this.$element.parent().parent()[0];

        if (liParent && liParent.tagName === 'LI') {
            let liElement = angular.element(liParent);
            liElement.addClass('hidden');
            this.$scope.$watch( () => this.isUpdating,
                newValue => (newValue) ? liElement.removeClass('hidden') : liElement.addClass('hidden')
            );
        }
    }

    $onDestroy() {
        this.isUpdatingSub.dispose();
    }

    updateStatus(message) {
        this.isUpdating = message.body;
    }
}
