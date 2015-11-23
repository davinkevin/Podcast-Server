/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import angular from 'angular';
import _ from 'lodash';
import AppRestangularConfig from '../../../config/restangular.config';

class StatService {

    constructor(Restangular) {
        "ngInject";
        this.Restangular = Restangular;
        this.base = this.Restangular.one('stats');
    }

    statsByType(numberOfMonth = 1) {
        return this.base.all('byType').post(numberOfMonth);
    }

    resetChart(chartSeries) {
        _.updateinplace(chartSeries, []);
    }

    dateMapper() {
        return (value) => { return { date : Date.UTC(value.date[0], value.date[1]-1, value.date[2]), numberOfItems : value.numberOfItems }; };
    }

    highChartsMapper() {
        return (value) => [value.date, value.numberOfItems];
    }

    mapToHighCharts(values) {
        return _(values)
            .map(this.dateMapper())
            .sortBy("date")
            .map(this.highChartsMapper())
            .value();
    }

    highChartsConfig(chartSeries) {
        return {
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
            series: chartSeries,
            title : {
                text : ''
            },
            credits: {
                enabled: false
            },
            loading: false
        };
    }
}

export default angular
    .module('ps.common.service.data.statsService', [
        AppRestangularConfig.name
    ])
    .service('statService', StatService);
