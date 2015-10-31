import angular from 'angular';
import AppHighChartsConfig from '../../../config/highCharts';
import PodcastDetailsStatsComponent from './stats.component';

export default angular
    .module('ps.podcasts.details.stats', [
        AppHighChartsConfig.name
    ])
    .directive('podcastStats', PodcastDetailsStatsComponent.component)
    .controller('PodcastDetailsStatsCtrl', PodcastDetailsStatsComponent);