class podcastStatsDirective {

    constructor() {
        this.restrict = 'E';
        this.scope = { podcast : '='};
        this.templateUrl = 'html/podcast-details-stats.html';
        this.controller = 'PodcastDetailsStatsCtrl';
        this.controllerAs = 'pdsc';
    }

}

class PodcastDetailsStatsCtrl {

    constructor($scope, $q, podcastService, numberOfMonthToShow) {
        let dateInThePast = this.getPastDate(numberOfMonthToShow);

        let dateMapper = (value) => { return { date : Date.UTC(value.date[0], value.date[1]-1, value.date[2]), numberOfItems : value.numberOfItems }; },
            highChartsMapper = (value) => [value.date, value.numberOfItems],
            timeFilter = (value) => value.date > dateInThePast;

        this.chartSeries = [];

        $q.all([
            podcastService.statsByByDownloaddate($scope.podcast.id),
            podcastService.statsByPubdate($scope.podcast.id)
        ]).then((arrayResult) => {
            this.chartSeries.push({"name": "Download Date", "data": _(arrayResult[0]).map(dateMapper).sortBy("date").filter(timeFilter).map(highChartsMapper).value()});
            this.chartSeries.push({"name": "Publication Date", "data": _(arrayResult[1]).map(dateMapper).sortBy("date").filter(timeFilter).map(highChartsMapper).value()});
        });

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
                text : 'Répartition par date'
            },
            credits: {
                enabled: false
            },
            loading: false,
        };
    }


    getPastDate(numberOfMonthToShow) {
        let dateInThePast = new Date(Date.now());
        dateInThePast.setMonth(dateInThePast.getMonth()-numberOfMonthToShow);
        return dateInThePast;
    }
}


angular.module('ps.podcast.details.stats', [
    'highcharts-ng'
])
    .directive('podcastStats', () => new podcastStatsDirective())
    .constant('numberOfMonthToShow', 6)
    .controller('PodcastDetailsStatsCtrl', PodcastDetailsStatsCtrl);