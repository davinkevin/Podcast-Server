angular.module('ps.podcast.creation', [
    'ps.config.route',
    'ps.dataservice',
    'ngTagsInput'
])
    .config(function($routeProvider, commonKey) {
        $routeProvider.
            when('/podcast-creation', {
                templateUrl: 'html/podcast-creation.html',
                controller: 'PodcastAddCtrl',
                hotkeys: commonKey
            });
    })
    .constant('defaultPodcast', { hasToBeDeleted : true, cover : { height: 200, width: 200 } })
    .controller('PodcastAddCtrl', function ($scope, $location, defaultPodcast, tagService, podcastService) {
        $scope.podcast = angular.extend(podcastService.getNewPodcast(), defaultPodcast );

        $scope.findInfo = function() {
            podcastService.findInfo($scope.podcast.url).then(function(podcastFetched) {
                $scope.podcast.title = podcastFetched.title;
                $scope.podcast.description = podcastFetched.description;
                $scope.podcast.type = podcastFetched.type;
                $scope.podcast.cover.url = podcastFetched.cover.url;
            })
        };
        
        $scope.loadTags = function(query) {
            return tagService.search(query);
        };

        $scope.changeType = function() {
            if (/beinsports\.fr/i.test($scope.podcast.url)) {
                $scope.podcast.type = "BeInSports";
            } else if (/canalplus\.fr/i.test($scope.podcast.url)) {
                $scope.podcast.type = "CanalPlus";
            } else if (/jeuxvideo\.fr/i.test($scope.podcast.url)) {
                $scope.podcast.type = "JeuxVideoFR";
            } else if (/jeuxvideo\.com/i.test($scope.podcast.url)) {
                $scope.podcast.type = "JeuxVideoCom";
            } else if (/parleys\.com/i.test($scope.podcast.url)) {
                $scope.podcast.type = "Parleys";
            } else if (/pluzz\.francetv\.fr/i.test($scope.podcast.url)) {
                $scope.podcast.type = "Pluzz";
            } else if (/youtube\.com/i.test($scope.podcast.url)) {
                $scope.podcast.type = "Youtube";
            } else if ($scope.podcast.url.length > 0) {
                $scope.podcast.type = "RSS";
            } else {
                $scope.podcast.type = "Send";
            }
        };

        $scope.save = function() {
            podcastService.save($scope.podcast).then(function (podcast) {
                $location.path('/podcast/' + podcast.id);
            });
        };
        
    });