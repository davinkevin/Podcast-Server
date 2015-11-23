import angular from 'angular';
import 'restangular';
import 'lodash';

export default angular
    .module('ps.config.restangular', ['restangular'])
    .config((RestangularProvider) => {
        "ngInject";
        RestangularProvider.setBaseUrl('/api/');
        RestangularProvider.addElementTransformer('items', false, (item) => {
            item.addRestangularMethod('reset', 'get', 'reset');
            item.addRestangularMethod('download', 'get', 'addtoqueue');
            return item;
        });
    });