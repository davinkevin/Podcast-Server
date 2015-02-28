angular.module('ps.podcast.details', [
    'ps.config.route',
    'ps.podcast.details',
    'ps.podcast.details.episodes',
    'ps.podcast.details.edition',
    'ps.podcast.details.upload',
    'restangular'
]).config(function($routeProvider, commonKey) {
    $routeProvider.
        when('/podcast/:podcastId', {
            templateUrl: 'html/podcast-detail.html',
            controller: 'PodcastDetailCtrl',
            controllerAs: 'pdc',
            hotkeys: [
                ['r', 'Refresh', 'pdc.refreshItems()'],
                ['f', 'Force Refresh', 'pdc.refresh()'],
                ['l', 'List of Items', 'pdc.podcastTabs[0].active = true'],
                ['m', 'Modification of Podcast', 'pdc.podcastTabs[1].active = true']
            ].concat(commonKey),
            resolve : {
                podcast : function (podcastService, $route) {
                    return podcastService.findById($route.current.params.podcastId);
                }
            }
        })    
})
    .controller('PodcastDetailCtrl', function ($scope, podcast, Restangular) {
        var vm = this;
        
        vm.podcast = podcast;
        vm.podcastTabs= [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : podcast.type !== 'send'}
        ];

        vm.refreshItems = function() {
            $scope.$broadcast('podcastItems:refresh');
        };
        
        vm.refresh = function () {
            Restangular.one("task").customPOST(vm.podcast.id, "updateManager/updatePodcast/force")
                .then(vm.refreshItems);
        };

        $scope.$on("podcastEdition:save", vm.refreshItems);
    });