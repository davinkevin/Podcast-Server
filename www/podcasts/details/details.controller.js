import {RouteConfig, View, HotKeys} from '../../decorators';
import template from './details.html!text';

@RouteConfig({
    path : '/podcasts/:podcastId',
    as : 'pdc',
    resolve : {
        podcast: (podcastService, $route) => {"ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
})
@HotKeys({})
@View({
    template : template
})
export default class PodcastDetailCtrl {

    constructor($scope, podcast, UpdateService, $timeout){
        "ngInject";
        this.$scope = $scope;
        this.UpdateService = UpdateService;
        this.podcast = podcast;
        this.$timeout = $timeout;

        this.podcastTabs = [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : this.podcast.type !== 'send'},
            { heading : 'Stats', active : false }
        ];
        this.$scope.$on("podcastEdition:save", () => this.refreshItems());
        this.$scope.$on("podcastEdition:upload", () => this.refreshItems());

        this.podcast.isUpdatable = function() {
            return this.type !== 'send';
        };
    }

    refreshItems() {
        this.$scope.$broadcast('podcastItems:refresh');
    }

    refresh() {
        this.UpdateService
            .forceUpdatePodcast(this.podcast.id)
            .then(() => this.refreshItems());
    }

    tabsActive(num) {
        this.podcastTabs[num].active = true;
    }

    chartReflow() {
        this.$timeout(() => {
            this.$scope.$broadcast('highchartsng.reflow');
        }, 10);
    }

    isUpdatable() {
        return this.podcast.isUpdatable();
    }
}
