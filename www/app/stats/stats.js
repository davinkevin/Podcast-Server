/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../decorators';
import {TitleService} from '../common/service/title.service';
import AppRouteConfig from '../config/route';
import StatsService from '../common/service/data/statsService';
import HighCharts from '../common/modules/highCharts';
import template from './stats.html!text';

@Module({
    name : 'ps.stats',
    modules : [ AppRouteConfig, HighCharts, StatsService, TitleService ]
})
@Component({
    selector : 'stats',
    as : 'sc',
    template : template,

    path : '/stats',
    resolve : {
        stats : statService => {"ngInject"; return statService.byDownloadDate();}
    }
})
export default class StatsController {

    month = 1;
    statsChoice = 'byDownloadDate';

    statsType = [
        { title : 'Download Date', method : 'byDownloadDate'},
        { title : 'Creation Date', method : 'byCreationDate'},
        { title : 'Publication Date', method : 'byPubDate'}
    ];

    constructor(statService, TitleService) {
        "ngInject";
        this.statService = statService;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.transform(this.stats);
        this.TitleService.title = 'Stats';
    }

    transform(stats) {
        this.chartSeries = stats.map((value) => ({ name : value.type, data : this.statService.mapToHighCharts(value.values) }));
        this.chartConfig = this.statService.highChartsConfig(this.chartSeries);
    }

    navigate(offset) {
        this.month += offset;
        this.generateChartData();
    }

    generateChartData() {
        return this.statService[this.statsChoice](this.month).then(statsByType => this.transform(statsByType));
    }
}

