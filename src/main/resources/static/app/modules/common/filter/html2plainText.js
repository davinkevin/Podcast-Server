angular.module('ps.common.filter.html2plainText', [])
    .filter('htmlToPlaintext', function () {
        return function(text) {
            return String(text || "").replace(/<[^>]+>/gm, '');
        };
    }
);