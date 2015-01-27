angular.module('ps.config.ngstomp', [
    'AngularStompDK'
])
    .config(function(ngstompProvider){
        ngstompProvider
            .url('/ws')
            .credential('login', 'password')
            .class(SockJS);
    });