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

    byDownloadDate(numberOfMonth = 1) {
        return this.$http.post('/api/stats/byDownloadDate', numberOfMonth).then(r => r.data);
    }

    byCreationDate(numberOfMonth = 1) {
        return this.$http.post('/api/stats/byCreationDate', numberOfMonth).then(r => r.data);
    }

    byPubDate(numberOfMonth = 1) {
        return this.$http.post('/api/stats/byPubDate', numberOfMonth).then(r => r.data);
    }

    dateMapper() {
        return value => ({ date : new Date(value.date).getTime(), numberOfItems : value.numberOfItems });
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
                chart: { type: 'spline'},
                plotOptions: { spline: { marker: { enabled: true}}
                },
                xAxis: {
                    type: 'datetime',
                    dateTimeLabelFormats: { month: '%e. %b', year: '%b'},
                    title: { text: 'Date'}
                },
                yAxis : { title : { text : '# of items' } }
            },
            series: chartSeries,
            title : { text: ''},
            credits: { enabled: false},
            loading: false
        };
    }
}