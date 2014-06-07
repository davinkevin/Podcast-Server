angular.module('podcast.filters', [])
    .filter('htmlToPlaintext', function () {
        return function(text) {
            return String(text).replace(/<[^>]+>/gm, '');
        };
    }
);