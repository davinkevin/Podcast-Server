angular.module('podcastFilters', [])
    .filter('momentDDMMYYYY', function() {
        return function(input) {
            return moment(input).format("DD/MM/YYYY");
        };
});