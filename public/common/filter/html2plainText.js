import angular from 'angular';

export default angular.module('ps.common.filter.html2plainText', [])
    .filter('htmlToPlaintext', () => (text) => String(text || "").replace(/<[^>]+>/gm, ''))
    .filter('cleanHtml', () => (text) => String(text || "").replace(' =""', ''));