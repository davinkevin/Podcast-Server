/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import _ from 'lodash';
import {RouteConfig, View, Module} from '../decorators';
import AppRouteConfig from '../config/route';
import StatsService from '../common/service/data/statsService';
import HighCharts from '../common/modules/highCharts';
import template from './stats.html!text';

@Module({
    name : 'ps.stats',
    modules : [
        AppRouteConfig,
        HighCharts,
        StatsService
    ]
})
@RouteConfig({
    path : '/stats',
    as : 'sc',
    resolve : {
        stats : statService => {"ngInject"; return statService.statsByType();}
    }
})
@View({
    template : template
})
export default class StatsController {

    constructor(statService, stats) {
        "ngInject";
        this.statService = statService;
        this.month = 1;

        this.chartSeries = [];
        this.transform(stats);
        this.chartConfig = statService.highChartsConfig(this.chartSeries);
    }

    transform(stats) {
        _.updateinplace(this.chartSeries, []);
        stats.map((value) => this.chartSeries.push({ name : value.type, data : this.statService.mapToHighCharts(value.values) }) );
    }

    navigate(offset) {
        this.month += offset;
        this.generateChartData();
    }

    generateChartData() {
        return this.statService
            .statsByType(this.month)
            .then(statsByType => this.transform(statsByType));
    }
}

