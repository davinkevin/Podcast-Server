/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import angular from 'angular';

export default angular.module('ps.config.videogular', [
    'ngSanitize',
    'com.2fdevs.videogular',
    'com.2fdevs.videogular.plugins.poster',
    'com.2fdevs.videogular.plugins.controls',
    'com.2fdevs.videogular.plugins.overlayplay',
    'com.2fdevs.videogular.plugins.buffering',
]);