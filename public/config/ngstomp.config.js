import angular from 'angular';
import Stomp from 'stomp-websocket';
import SockJS from 'sockjs-client';
import 'AngularStompDK';

export default angular
    .module('ps.config.ngstomp', ['AngularStompDK'])
    .config((ngstompProvider) => ngstompProvider.url('/ws').credential('login', 'password').class(SockJS));