import {RouteConfig, View, HotKeys, Module} from '../../decorators';
import StatsModule from './stats/stats';
import EpisodesModule from './episodes/episodes';
import EditionModule from './edition/edition';
import UploadModule from './upload/upload';
import AppRouteConfig from '../../config/route';
import UpdateService from '../../common/service/data/updateService';
import PodcastService from '../../common/service/data/podcastService';
import template from './details.html!text';


@Module({
    name : 'ps.podcasts.details',
    modules : [
        AppRouteConfig,
        StatsModule,
        EpisodesModule,
        EditionModule,
        UploadModule,
        UpdateService,
        PodcastService
    ]
})
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

    constructor($scope, podcast, podcastService, $timeout){
        "ngInject";
        this.$scope = $scope;
        this.podcastService = podcastService;
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
        this.podcastService
            .forceRefresh(this.podcast.id)
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
