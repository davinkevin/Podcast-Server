import angular from 'angular';
import HighChartsNg from 'highcharts-ng';
import PodcastDetailsStatsComponent from './stats.component';

export default angular
    .module('ps.podcasts.details.stats', [
        HighChartsNg
    ])
    .directive('podcastStats', PodcastDetailsStatsComponent.component)
    .controller('PodcastDetailsStatsCtrl', PodcastDetailsStatsComponent);