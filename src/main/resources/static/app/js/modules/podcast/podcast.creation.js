class PodcastCreationController {

    constructor($location, defaultPodcast, tagService, podcastService) {
        this.podcastService = podcastService;
        this.$location = $location;
        this.tagService = tagService;
        this.podcast = angular.extend(this.podcastService.getNewPodcast(), defaultPodcast );
    }

    findInfo() {
        return this.podcastService.findInfo(this.podcast.url)
            .then((podcastFetched) => {
                this.podcast.title = podcastFetched.title;
                this.podcast.description = podcastFetched.description;
                this.podcast.type = podcastFetched.type;
                this.podcast.cover.url = podcastFetched.cover.url;
            });
    }

    loadTags(query) {
        return this.tagService.search(query);
    }

    changeType() {
        if (/beinsports\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "BeInSports";
        } else if (/canalplus\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "CanalPlus";
        } else if (/jeuxvideo\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "JeuxVideoFR";
        } else if (/jeuxvideo\.com/i.test(this.podcast.url)) {
            this.podcast.type = "JeuxVideoCom";
        } else if (/parleys\.com/i.test(this.podcast.url)) {
            this.podcast.type = "Parleys";
        } else if (/pluzz\.francetv\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "Pluzz";
        } else if (/youtube\.com/i.test(this.podcast.url)) {
            this.podcast.type = "Youtube";
        } else if (this.podcast.url.length > 0) {
            this.podcast.type = "RSS";
        } else {
            this.podcast.type = "Send";
        }
    }

    save() {
        this.podcastService.save(this.podcast)
            .then((podcast) => this.$location.path('/podcast/' + podcast.id));
    }

}

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
                controllerAs: 'pac',
                hotkeys: commonKey
            });
    })
    .constant('defaultPodcast', { hasToBeDeleted : true, cover : { height: 200, width: 200 } })
    .controller('PodcastAddCtrl', PodcastCreationController);