import angular from 'angular';
import AngularStompDKConfig from 'config/ngstomp.config';
import UpdatingComponent from './updating.component'


export default angular
    .module('ps.common.component.updating', [
        AngularStompDKConfig.name
    ])
    .directive('updateStatus', UpdatingComponent.component)
    .controller('UpdatingController', UpdatingComponent);
