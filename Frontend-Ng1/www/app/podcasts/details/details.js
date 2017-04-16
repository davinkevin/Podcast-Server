import {Component, Module} from '../../decorators';
import {TitleService} from '../../common/service/title.service';
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
    modules : [AppRouteConfig, StatsModule, EpisodesModule, EditionModule, UploadModule, UpdateService, PodcastService, TitleService]
})
@Component({
    selector : 'podcasts-detail',
    template : template,
    as : 'pdc',

    path : '/podcasts/:podcastId',
    resolve : {
        podcast: (podcastService, $route) => {"ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
})
export default class PodcastDetailCtrl {

    totalItems = null;

    constructor($scope, podcastService, $timeout, TitleService){
        "ngInject";
        this.$scope = $scope;
        this.podcastService = podcastService;
        this.$timeout = $timeout;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.TitleService.title = this.podcast.title;

        this.$scope.$on("podcastEdition:save", () => this.refreshItems());
        this.$scope.$on("podcastEdition:upload", () => this.refreshItems());

        this.podcastTabs = [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : this.podcast.type !== 'upload'},
            { heading : 'Stats', active : false }
        ];

        this.podcast.isUpdatable = function() { return this.type !== 'upload'; };

        if (this.podcast.isUpdatable() && !this.podcast.lastUpdate)
            this.refresh();
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
        this.$timeout(() => { this.$scope.$broadcast('highchartsng.reflow'); }, 10);
    }

    isUpdatable() {
        return this.podcast.isUpdatable();
    }

    updateNumberOfEpisodes(num){
        this.totalItems = num;
    }
}
