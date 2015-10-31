/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */

import template from './stats.html!text';

export default class StatsController {

    constructor(statService, stats) {
        "ngInject";
        this.statService = statService;
        this.month = 1;

        this.chartSeries = [];
        this.transform(stats);

        this.chartConfig = {
            options: {
                chart: {
                    type: 'spline'
                },
                plotOptions: {
                    spline: {
                        marker: {
                            enabled: true
                        }
                    }
                },
                xAxis: {
                    type: 'datetime',
                    dateTimeLabelFormats: { // don't display the dummy year
                        month: '%e. %b',
                        year: '%b'
                    },
                    title: {
                        text: 'Date'
                    }
                }
            },
            series: this.chartSeries,
            title : {
                text : ''
            },
            credits: {
                enabled: false
            },
            loading: false
        };
    }

    transform(stats) {
        _.updateinplace(this.chartSeries, []);

        angular.forEach(stats, (value) => {
            let element = { name : value.type };
            element.data = _(value.values)
                .map(StatsController.dateMapper())
                .sortBy("date")
                .map(StatsController.highChartsMapper())
                .value();
            this.chartSeries.push(element);
        });
    }

    static dateMapper() {
        return (value) => { return { date : Date.UTC(value.date[0], value.date[1]-1, value.date[2]), numberOfItems : value.numberOfItems }; };
    }

    static highChartsMapper() {
        return (value) => [value.date, value.numberOfItems];
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

    static routeConfig($routeProvider, commonKey) {
        "ngInject";
        $routeProvider.
            when('/stats', {
                template: template,
                controller: 'StatsController',
                controllerAs: 'sc',
                hotkeys: commonKey,
                resolve : {
                    stats : statService => statService.statsByType()
                }
            });
    }
}

