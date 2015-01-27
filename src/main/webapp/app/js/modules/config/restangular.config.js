angular.module('ps.config.restangular', [
    'restangular'
])
    .config(function(RestangularProvider) {
        RestangularProvider.setBaseUrl('/api/');

        RestangularProvider.addElementTransformer('items', false, function(item) {
            item.addRestangularMethod('reset', 'get', 'reset');
            item.addRestangularMethod('download', 'get', 'addtoqueue');
            return item;
        });
    });