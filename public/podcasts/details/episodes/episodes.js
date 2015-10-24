import angular from 'angular';
import HtmlFilters from 'common/filter/html2plainText';
import PlaylistService from 'common/service/playlistService';
import PodcastItemsListComponent from './episodes.component.js';



export default angular.module('ps.podcasts.details.episodes', [
    PlaylistService.name,
    HtmlFilters.name
])
    .directive('podcastItemsList', PodcastItemsListComponent.component)
    .constant('PodcastItemPerPage', 10)
    .controller('PodcastItemsListCtrl', PodcastItemsListComponent);
