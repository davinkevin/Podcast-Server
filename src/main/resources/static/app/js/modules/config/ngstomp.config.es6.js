angular
    .module('ps.config.ngstomp', ['AngularStompDK'])
    .config((ngstompProvider) => ngstompProvider.url('/ws').credential('login', 'password').class(SockJS));