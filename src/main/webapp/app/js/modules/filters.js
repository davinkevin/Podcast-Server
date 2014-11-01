angular.module('ps.filters', [])
    .filter('htmlToPlaintext', function () {
        return function(text) {
            return String(text || "").replace(/<[^>]+>/gm, '');
        };
    }
);