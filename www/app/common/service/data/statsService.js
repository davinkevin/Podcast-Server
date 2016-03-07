/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.data.statsService'
})
@Service('statService')
export default class StatService {

    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    statsByType(numberOfMonth = 1) {
        return this.$http.post('/api/stats/byType', numberOfMonth).then(r => r.data);
    }

    dateMapper() {
        return value => ({ date : Date.UTC(value.date[0], value.date[1]-1, value.date[2]), numberOfItems : value.numberOfItems });
    }

    highChartsMapper() {
        return value => [value.date, value.numberOfItems];
    }

    sortByDate() {
        return (a, b) => a.date > b.date ? 1 : a.date < b.date ? -1 : 0;
    }

    mapToHighCharts(values) {
        return values
            .map(this.dateMapper())
            .sort(this.sortByDate())
            .map(this.highChartsMapper());
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