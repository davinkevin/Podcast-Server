import {Component, Module} from '../../../decorators';
import HighCharts from '../../../common/modules/highCharts';
import PodcastDataService from '../../../common/service/data/podcastService';
import StatsDataService from '../../../common/service/data/statsService';
import template from './stats.html!text';

@Module({
    name : 'ps.podcasts.details.stats',
    modules : [ HighCharts, PodcastDataService, StatsDataService ]
})
@Component({
    selector : 'podcast-stats',
    as : 'pdsc',
    bindings : { podcast : '='},
    template : template
})
export default class PodcastDetailsStatsComponent {

    constructor($scope, $q, podcastService, statService) {
        "ngInject";
        this.$q = $q;
        this.podcastService = podcastService;
        this.statService = statService;
        this.month = 6;
        this.chartSeries = [];

        this.generateChartData();

        $scope.$on("podcastItems:refresh", () => this.generateChartData());
    }

    navigate(offset) {
        this.month += offset;
        return this.generateChartData();
    }

    generateChartData() {
        this.chartSeries = [];

        return this.$q.all([
                this.podcastService.statsByByDownloadDate(this.podcast.id, this.month),
                this.podcastService.statsByPubDate(this.podcast.id, this.month),
                this.podcastService.statsByCreationDate(this.podcast.id, this.month)
            ])
            .then((arrayResult) => {
                this.chartSeries.push({"name": "Download Date", "data": this.statService.mapToHighCharts(arrayResult[0])});
                this.chartSeries.push({"name": "Publication Date", "data": this.statService.mapToHighCharts(arrayResult[1])});
                this.chartSeries.push({"name": "Creation Date", "data": this.statService.mapToHighCharts(arrayResult[2])});
                return this.chartSeries;
            })
            .then(chartSeries => this.chartConfig = this.statService.highChartsConfig(chartSeries));
    }
}