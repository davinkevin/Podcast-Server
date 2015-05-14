(function () {
    'use strict';
    var _createClass = function () {
        function defineProperties(target, props) {
            for (var i = 0; i < props.length; i++) {
                var descriptor = props[i];
                descriptor.enumerable = descriptor.enumerable || false;
                descriptor.configurable = true;
                if ('value' in descriptor)
                    descriptor.writable = true;
                Object.defineProperty(target, descriptor.key, descriptor);
            }
        }
        return function (Constructor, protoProps, staticProps) {
            if (protoProps)
                defineProperties(Constructor.prototype, protoProps);
            if (staticProps)
                defineProperties(Constructor, staticProps);
            return Constructor;
        };
    }();
    var _classCallCheck = function (instance, Constructor) {
        if (!(instance instanceof Constructor)) {
            throw new TypeError('Cannot call a class as a function');
        }
    };
    angular.module('podcastApp', [
        'ps.search',
        'ps.podcast',
        'ps.item',
        'ps.download',
        'ps.player',
        'ps.stats',
        'ps.common',
        'ps.dataservice',
        'ps.config',
        'ps.partial'
    ]);
    var authorizeNotificationDirective = function authorizeNotificationDirective() {
        _classCallCheck(this, authorizeNotificationDirective);
        this.replace = true;
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'html/authorize-notification.html';
        this.controllerAs = 'an';
        this.controller = 'authorizeNotificationController';
    };
    var authorizeNotificationController = function () {
        function authorizeNotificationController($window, $notification) {
            _classCallCheck(this, authorizeNotificationController);
            this.$window = $window;
            this.$notification = $notification;
            this.state = this.hasToBeShown();
        }
        authorizeNotificationController.$inject = ["$window", "$notification"];
        _createClass(authorizeNotificationController, [
            {
                key: 'manuallyactivate',
                value: function manuallyactivate() {
                    var _this = this;
                    this.$notification.requestPermission().then(function () {
                        _this.state = _this.hasToBeShown();
                    });
                }
            },
            {
                key: 'hasToBeShown',
                value: function hasToBeShown() {
                    return 'Notification' in this.$window && this.$window.Notification.permission != 'granted';
                }
            }
        ]);
        return authorizeNotificationController;
    }();
    angular.module('authorize-notification', ['notification']).directive('authorizeNotification', function () {
        return new authorizeNotificationDirective();
    }).controller('authorizeNotificationController', authorizeNotificationController);
    angular.module('ps.common', [
        'ps.filters',
        'navbar',
        'authorize-notification',
        'device-detection'
    ]);
    var deviceDetectorService = function () {
        function deviceDetectorService($window) {
            _classCallCheck(this, deviceDetectorService);
            this.$window = $window;
        }
        deviceDetectorService.$inject = ["$window"];
        _createClass(deviceDetectorService, [{
                key: 'isTouchedDevice',
                value: function isTouchedDevice() {
                    return 'ontouchstart' in this.$window;
                }
            }]);
        return deviceDetectorService;
    }();
    angular.module('device-detection', []).service('deviceDetectorService', deviceDetectorService);
    angular.module('ps.filters', []).filter('htmlToPlaintext', function () {
        return function (text) {
            return String(text || '').replace(/<[^>]+>/gm, '');
        };
    });
    /**
 * Created by kevin on 14/08/2014.
 */
    _.mixin({
        // Update in place, does not preserve order
        updateinplace: function updateinplace(localArray, remoteArray) {
            var comparisonFunction = arguments[2] === undefined ? function (inArray, elem) {
                return inArray.indexOf(elem);
            } : arguments[2];
            var withOrder = arguments[3] === undefined ? false : arguments[3];
            // Remove from localArray what is not in the remote array :
            _.forEachRight(localArray.slice(), function (elem, key) {
                return comparisonFunction(remoteArray, elem) === -1 && localArray.splice(key, 1);
            });
            // Add to localArray what is new in the remote array :
            _.forEach(remoteArray, function (elem) {
                return comparisonFunction(localArray, elem) === -1 && localArray.push(elem);
            });
            if (withOrder) {
                _.forEach(remoteArray, function (elem, key) {
                    var elementToMove = localArray.splice(comparisonFunction(localArray, elem), 1)[0];
                    localArray.splice(key, 0, elementToMove);
                });
            }
            return localArray;
        }
    });
    var navbarController = function navbarController() {
        _classCallCheck(this, navbarController);
        this.navCollapsed = true;
    };
    var navbarDirective = function () {
        function navbarDirective() {
            _classCallCheck(this, navbarDirective);
            this.transclude = true;
            this.replace = true;
            this.restrict = 'E';
            this.scope = true;
            this.templateUrl = 'html/navbar.html';
            this.controller = 'navbarController';
            this.controllerAs = 'navbar';
        }
        _createClass(navbarDirective, [{
                key: 'link',
                value: function link(scope, element) {
                    element.removeClass('hidden');
                }
            }]);
        return navbarDirective;
    }();
    angular.module('navbar', []).directive('navbar', function () {
        return new navbarDirective();
    }).controller('navbarController', navbarController);
    angular.module('ps.config', [
        'ps.config.route',
        'ps.config.loading',
        'ps.config.restangular',
        'ps.config.ngstomp',
        'ps.config.module'
    ]);
    angular.module('ps.config.loading', ['angular-loading-bar']).config(["cfpLoadingBarProvider", function (cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    }]);
    angular.module('ps.config.module', [
        'ngTouch',
        'ngAnimate',
        'ui.bootstrap',
        'truncate'
    ]);
    angular.module('ps.config.ngstomp', ['AngularStompDK']).config(["ngstompProvider", function (ngstompProvider) {
        return ngstompProvider.url('/ws').credential('login', 'password')['class'](SockJS);
    }]);
    angular.module('ps.config.restangular', ['restangular']).config(["RestangularProvider", function (RestangularProvider) {
        RestangularProvider.setBaseUrl('/api/');
        RestangularProvider.addElementTransformer('items', false, function (item) {
            item.addRestangularMethod('reset', 'get', 'reset');
            item.addRestangularMethod('download', 'get', 'addtoqueue');
            return item;
        });
    }]);
    angular.module('ps.config.route', [
        'ngRoute',
        'cfp.hotkeys'
    ]).constant('commonKey', [
        [
            'h',
            'Goto Home',
            function (event) {
                event.preventDefault();
                window.location.href = '#/items';
            }
        ],
        [
            's',
            'Goto Search',
            function (event) {
                event.preventDefault();
                window.location.href = '#/item/search';
            }
        ],
        [
            'p',
            'Goto Podcast List',
            function (event) {
                event.preventDefault();
                window.location.href = '#/podcasts';
            }
        ],
        [
            'd',
            'Goto Download List',
            function (event) {
                event.preventDefault();
                window.location.href = '#/download';
            }
        ]
    ]).config(["$routeProvider", function ($routeProvider) {
        return $routeProvider.otherwise({ redirectTo: '/items' });
    }]).config(["$locationProvider", function ($locationProvider) {
        return $locationProvider.html5Mode(true);
    }]);
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/authorize-notification.html', '<div ng-show="an.state" class="alert alert-info text-center" role="alert">\n' + '    <a ng-click="an.manuallyactivate()" class="btn btn-primary">Activer Notification</a>\n' + '</div>\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/download.html', '<div class="container downloadList">\n' + '\n' + '    <div class="row form-horizontal" style="margin-top: 15px;">\n' + '        <div class="col-xs-offset-1 col-md-offset-1 col-sm-offset-1 col-lg-offset-1 form-group col-md-6 col-lg-6 col-xs-6 col-sm-6 ">\n' + '            <label class="pull-left control-label">T\xE9l\xE9chargements simultan\xE9s</label>\n' + '            <div class="col-md-3 col-lg-3 col-xs-3 col-sm-3">\n' + '                <input ng-model="dc.numberOfSimDl" ng-change="dc.updateNumberOfSimDl(numberOfSimDl)" type="number" class="form-control" placeholder="Number of download">\n' + '            </div>\n' + '        </div>\n' + '        <div class="btn-group pull-right">\n' + '            <button ng-click="dc.restartAllDownload()" type="button" class="btn btn-default">D\xE9marrer</button>\n' + '            <button ng-click="dc.pauseAllDownload()" type="button" class="btn btn-default">Pause</button>\n' + '            <button ng-click="dc.stopAllDownload()" type="button" class="btn btn-default">Stop</button>\n' + '        </div>\n' + '    </div>\n' + '    <div class="media"  ng-repeat="item in dc.items | orderBy:\'-progression\' track by item.id" >\n' + '\n' + '        <div class="buttonList pull-right">\n' + '            <br/>\n' + '            <button ng-click="dc.toggleDownload(item)" type="button" class="btn btn-sm"\n' + '                    ng-class="{\'btn-primary\' : item.status === \'Started\', \'btn-warning\' : item.status === \'Paused\'}"><i class="glyphicon glyphicon-play"></i><i class="glyphicon glyphicon-pause"></i></button>\n' + '            <button ng-click="dc.stopDownload(item)" type="button" class="btn btn-danger btn-sm"><span class="glyphicon glyphicon-stop"></span></button>\n' + '        </div>\n' + '\n' + '        <a class="pull-left" ng-href="/podcasts/{{item.podcastId}}/item/{{item.id}}">\n' + '            <img ng-src="{{item.cover.url}}" >\n' + '        </a>\n' + '\n' + '        <div class="media-body">\n' + '            <h5 class="media-heading">{{item.title | characters:100}}</h5>\n' + '            <br/>\n' + '            <progressbar class="progress-striped active" animate="false" value="item.progression" type="{{ dc.getTypeFromStatus(item) }}">{{item.progression}}%</progressbar>\n' + '        </div>\n' + '    </div>\n' + '\n' + '\n' + '    <br/>\n' + '\n' + '    <accordion close-others="true" ng-show="dc.waitingitems.length > 0">\n' + '        <accordion-group is-open="true">\n' + '            <accordion-heading>\n' + '                Liste d\'attente <span class="pull-right badge">{{ dc.waitingitems.length }}</span>\n' + '            </accordion-heading>\n' + '            <div class="media item-in-waiting-list clearfix"  ng-repeat="item in dc.waitingitems track by item.id"  >\n' + '\n' + '                <div class="pull-right">\n' + '                    <br/>\n' + '                    <button ng-click="dc.removeFromQueue(item)" type="button" class="btn btn-primary btn-sm"><i class="glyphicon glyphicon-minus"></i></button>\n' + '                    <button ng-click="dc.dontDonwload(item)" type="button" class="btn btn-danger btn-sm"><i class="glyphicon glyphicon-stop"></i></button>\n' + '                    <div class="btn-group" dropdown is-open="isopen" ng-show="dc.waitingitems.length > 1">\n' + '                        <button type="button" class="btn btn-default dropdown-toggle" dropdown-toggle><i class="ionicons ion-android-more"></i></button>\n' + '                        <ul class="dropdown-menu" role="menu">\n' + '                            <li ng-hide="$first"><a ng-click="dc.moveInWaitingList(item, 0)"><span class="fa fa-angle-double-up"></span> Premier</a></li>\n' + '                            <li><a ng-hide="$first || $index === 1" ng-click="dc.moveInWaitingList(item, $index-1)"><span class="fa fa-angle-up"></span> Monter</a></li>\n' + '                            <li><a ng-hide="$last || $index === dc.waitingitems.length-2" ng-click="dc.moveInWaitingList(item, $index+1)"><span class="fa fa-angle-down"></span> Descendre</a></li>\n' + '                            <li><a ng-hide="$last" ng-click="dc.moveInWaitingList(item, dc.waitingitems.length-1   )"><span class="fa fa-angle-double-down"></span> Dernier</a></li>\n' + '                        </ul>\n' + '                    </div>\n' + '                </div>\n' + '\n' + '                <a class="pull-left" ng-href="/podcasts/{{item.podcastId}}/item/{{item.id}}">\n' + '                    <img ng-src="{{item.cover.url}}">\n' + '                </a>\n' + '\n' + '                <div class="media-body">\n' + '                    <h5 class="media-heading">{{item.title | characters:100}}</h5>\n' + '                </div>\n' + '            </div>\n' + '\n' + '        </accordion-group>\n' + '    </accordion>\n' + '\n' + '\n' + '</div>');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/item-detail.html', '\n' + '<div class="container item-details">\n' + '\n' + '    <br/>\n' + '    <ol class="breadcrumb">\n' + '        <li><a href="/podcasts">Podcasts</a></li>\n' + '        <li><a ng-href="/podcasts/{{ idc.item.podcast.id }}"> {{ idc.item.podcast.title }}</a></li>\n' + '        <li class="active">{{ idc.item.title }}</li>\n' + '    </ol>\n' + '\n' + '    <div>\n' + '        <div class="col-xs-12 col-sm-12 col-md-3 col-lg-3">\n' + '            <div class="thumbnail">\n' + '                <a ng-href="{{ idc.item.proxyURL || idc.item.url }}" target="_self">\n' + '                    <img class="center-block" ng-src="{{idc.item.cover.url}}" width="200" height="200">\n' + '                </a>\n' + '\n' + '                <div class="caption">\n' + '\n' + '                    <div class="buttonList text-center">\n' + '                        <!-- T\xE9l\xE9chargement en cours -->\n' + '                        <span ng-show="idc.item.status == \'Started\' || idc.item.status == \'Paused\'" >\n' + '                            <button ng-click="idc.toggleDownload(idc.item)" type="button" class="btn btn-primary "><i class="glyphicon glyphicon-play"></i><i class="glyphicon glyphicon-pause"></i></button>\n' + '                            <button ng-click="idc.stopDownload(idc.item)" type="button" class="btn btn-danger"><span class="glyphicon glyphicon-stop"></span></button>\n' + '                        </span>\n' + '\n' + '                        <!-- Lancer le t\xE9l\xE9chargement -->\n' + '                        <button ng-click="idc.item.download()" ng-show="(idc.item.status != \'Started\' && idc.item.status != \'Paused\' ) && !idc.item.isDownloaded" type="button" class="btn btn-primary"><span class="glyphicon glyphicon-save"></span></button>\n' + '\n' + '                        <a ng-href="/podcasts/{{ idc.item.podcast.id }}/item/{{ idc.item.id }}/play" ng-show="idc.item.isDownloaded" type="button" class="btn btn-success"><span class="ionicons ion-social-youtube"></span></a>\n' + '\n' + '                        <!-- Add to Playlist -->\n' + '                        <a ng-show="idc.item.isDownloaded" ng-click="idc.toggleInPlaylist()" type="button" class="btn btn-primary">\n' + '                            <span ng-hide="idc.isInPlaylist()" class="glyphicon glyphicon-plus"></span>\n' + '                            <span ng-show="idc.isInPlaylist()" class="glyphicon glyphicon-minus"></span>\n' + '                        </a>\n' + '\n' + '                        <div class="btn-group" dropdown is-open="isopen">\n' + '                            <button type="button" class="btn btn-default dropdown-toggle" dropdown-toggle><i class="ionicons ion-android-more"></i></button>\n' + '                            <ul class="dropdown-menu dropdown-menu-right" role="menu">\n' + '                                <li ng-show="idc.item.isDownloaded"><a target="_self" ng-href="{{ idc.item.proxyURL }}"><span class="glyphicon glyphicon-play text-success"></span> Lire</a></li>\n' + '                                <li><a ng-click="idc.remove(idc.item)" ng-show="(idc.item.status != \'Started\' && idc.item.status != \'Paused\' )"><span class="glyphicon glyphicon-remove text-danger"></span> Retirer</a></li>\n' + '                                <li><a ng-href="{{ idc.item.url }}" target="_self"><span class="glyphicon glyphicon-globe text-info"></span> Lire en ligne</a></li>\n' + '                                <li><a ng-click="idc.reset(idc.item)"><span class="glyphicon glyphicon-repeat"></span> Reset</a></li>\n' + '                            </ul>\n' + '                        </div>\n' + '                        \n' + '                    </div>\n' + '                </div>\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <div class="col-xs-12 col-sm-12 col-md-9 col-lg-9">\n' + '            <div class="panel panel-default">\n' + '                <div class="panel-heading">\n' + '                    <h3 class="panel-title">{{ idc.item.title }}</h3>\n' + '                </div>\n' + '                <div class="panel-body">\n' + '                    {{ idc.item.description | htmlToPlaintext }}\n' + '                </div>\n' + '                <div class="panel-footer">Date de publication : <strong>{{idc.item.pubdate | date : \'dd/MM/yyyy \xE0 HH:mm\' }}</strong></div>\n' + '            </div>\n' + '        </div>\n' + '\n' + '    </div>\n' + '</div>\n' + '\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/item-player.html', '<div class="container item-player">\n' + '    <br/>\n' + '    <ol class="breadcrumb">\n' + '        <li><a href="/podcasts">Podcasts</a></li>\n' + '        <li><a ng-href="/podcasts/{{ ipc.item.podcast.id }}"> {{ ipc.item.podcast.title }}</a></li>\n' + '        <li class="active"><a ng-href="/podcasts/{{ ipc.item.podcast.id }}/item/{{ ipc.item.id }}">{{ ipc.item.title }}</a></li>\n' + '    </ol>\n' + '\n' + '    <div ng-show="ipc.item.isDownloaded" class="videogular-container">\n' + '        <videogular vg-theme="ipc.config.theme.url" vg-auto-play="ipc.config.autoPlay" >\n' + '            <vg-media vg-src="ipc.config.sources" vg-native-controls="false"></vg-media>\n' + '\n' + '            <vg-controls vg-autohide="ipc.config.sources[0].type.indexOf(\'audio\') === -1 && ipc.config.plugins.controls.autoHide" vg-autohide-time="ipc.config.plugins.controls.autoHideTime">\n' + '                <vg-play-pause-button></vg-play-pause-button>\n' + '                <vg-time-display>{{ currentTime | date:\'mm:ss\' }}</vg-time-display>\n' + '                <vg-scrub-bar>\n' + '                    <vg-scrub-bar-current-time></vg-scrub-bar-current-time>\n' + '                </vg-scrub-bar>\n' + '                <vg-time-display>{{ timeLeft | date:\'mm:ss\' }}</vg-time-display>\n' + '                <vg-volume>\n' + '                    <vg-mute-button></vg-mute-button>\n' + '                    <vg-volume-bar></vg-volume-bar>\n' + '                </vg-volume>\n' + '                <vg-fullscreen-button ng-show="ipc.config.sources[0].type.indexOf(\'audio\') === -1"></vg-fullscreen-button>\n' + '                <div class=\'btn-video-share\'><a target="_self" ng-href="{{ ipc.item.proxyURL }}" class="ionicons ion-android-share"></a></div>\n' + '            </vg-controls>\n' + '\n' + '            <vg-overlay-play></vg-overlay-play>\n' + '            \n' + '            <vg-poster vg-url=\'ipc.config.plugins.poster\'></vg-poster>\n' + '        </videogular>\n' + '    </div>\n' + '</div>');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/items-search.html', '\n' + '<div class="container item-listing" ng-swipe-right="isc.swipePage(-1)" ng-swipe-left="isc.swipePage(1)">\n' + '    <!--<div class="col-xs-11 col-sm-11 col-lg-11 col-md-11">-->\n' + '\n' + '    <div class="form-inline search-bar row" ng-show="isc.search">\n' + '        <div class="form-group col-sm-3 col-md-3 col-lg-3">\n' + '            <div class="input-group">\n' + '                <input type="text" class="form-control" ng-model="isc.searchParameters.term" placeholder="Recherche globale" ng-change="isc.resetSearch()" ng-model-options="{ debounce: 500 }">\n' + '                <span class="input-group-addon" ng-click="isc.searchParameters.term = \'\'; isc.resetSearch()"><i class="ionicons ion-android-cancel"></i></span>\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <div class="form-group col-sm-3 col-md-5 col-lg-5">\n' + '            <div class="input-group input-group-tags-input">\n' + '                <tags-input placeholder="Search by Tags" add-from-autocomplete-only="true" ng-model="isc.searchParameters.tags" display-property="name" class="bootstrap" on-tag-added="isc.resetSearch()" on-tag-removed="isc.resetSearch()">\n' + '                    <auto-complete source="isc.loadTags($query)" min-length="2"></auto-complete>\n' + '                </tags-input>\n' + '                <span class="input-group-addon" ng-click="isc.searchParameters.tags = []; isc.resetSearch()"><i class="ionicons ion-android-cancel"></i></span>\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <div class="form-group col-sm-3 col-md-2 col-lg-2">\n' + '            <select class="form-control" ng-model="isc.searchParameters.properties" ng-change="isc.resetSearch()">\n' + '                <option value>Tri</option>\n' + '                <option value="pertinence">Pertinence</option>\n' + '                <option value="pubdate">Date publication</option>\n' + '                <option value="downloadDate">Date de download</option>\n' + '            </select>\n' + '        </div>\n' + '\n' + '        <div class="form-group col-sm-3 col-md-2 col-lg-2">\n' + '            <!--<select class="form-control" ng-model="searchParameters.direction" ng-change="changePage()" ng-disabled="searchParameters.properties === \'pertinence\'">-->\n' + '            <select class="form-control" ng-model="isc.searchParameters.direction" ng-change="isc.resetSearch()">\n' + '                <option value>Ordre</option>\n' + '                <option value="DESC">Descendant</option>\n' + '                <option value="ASC">Ascendant</option>\n' + '            </select>\n' + '        </div>\n' + '    </div>\n' + '\n' + '    <div class="text-center row" >\n' + '        <pagination ng-show="isc.totalPages > 1" items-per-page="12" max-size="10" boundary-links="true" total-items="isc.totalItems" ng-model="isc.currentPage" ng-change="isc.changePage()" class="pagination pagination-centered" previous-text="&lsaquo;" next-text="&rsaquo;" first-text="&laquo;" last-text="&raquo;"></pagination>\n' + '        <a ng-click="isc.search = !isc.search;" ng-class="{\'btn-primary\' : isc.search, \'btn-default\' : !isc.search}" class="btn pull-right search-button"><i class="glyphicon glyphicon-search"></i></a>\n' + '    </div>\n' + '    <div class="row">\n' + '        <div ng-repeat="item in isc.items track by item.id" class="col-lg-3  col-md-3 col-sm-4 col-xs-6 itemInList">\n' + '            <div class="box">\n' + '                <div class="">\n' + '                    <img ng-class="{\'img-grayscale\' : (!item.isDownloaded) }" ng-src="{{ item.cover.url }}" alt="" class="img-responsive" />\n' + '                    <div class="overlay-button">\n' + '                        <div class="btn-group" dropdown>\n' + '                            <button type="button" class="btn dropdown dropdown-toggle" dropdown-toggle><i class="ionicons ion-android-more"></i></button>\n' + '                            <ul class="dropdown-menu dropdown-menu-right" role="menu">\n' + '                                <li ng-show="item.status == \'Started\' || item.status == \'Paused\'">\n' + '                                    <a ng-show="item.status == \'Started\'" ng-click="isc.toggleDownload(item)"><i class="glyphicon glyphicon-play text-primary"></i><i class="glyphicon glyphicon-pause text-primary"></i> Mettre en pause</a>\n' + '                                    <a ng-show="item.status == \'Paused\'" ng-click="isc.toggleDownload(item)"><i class="glyphicon glyphicon-play text-primary"></i><i class="glyphicon glyphicon-pause text-primary"></i> Reprendre</a>\n' + '                                </li>\n' + '                                <li ng-show="item.status == \'Started\' || item.status == \'Paused\'">\n' + '                                    <a ng-click="isc.stopDownload(item)">\n' + '                                        <span class="glyphicon glyphicon-stop text-danger"></span> Stopper\n' + '                                    </a>\n' + '                                </li>\n' + '                                <li ng-show="(item.status != \'Started\' && item.status != \'Paused\' ) && !item.isDownloaded">\n' + '                                    <a ng-click="item.download()">\n' + '                                        <span class="glyphicon glyphicon-save text-primary"></span> T\xE9l\xE9charger\n' + '                                    </a>\n' + '                                </li>\n' + '                                <li>\n' + '                                    <a ng-href="/podcasts/{{ item.podcastId }}/item/{{ item.id }}/play" ng-show="item.isDownloaded">\n' + '                                        <span class="ionicons ion-social-youtube text-success"></span> Lire dans le player\n' + '                                    </a>\n' + '                                </li>\n' + '                                <li ng-show="item.isDownloaded">\n' + '                                    <a ng-click="isc.addOrRemove(item)">\n' + '                                        <span ng-hide="isc.isInPlaylist(item)"><span class="glyphicon glyphicon-plus text-primary"></span> Ajouter \xE0 la Playlist</span>\n' + '                                        <span ng-show="isc.isInPlaylist(item)"><span class="glyphicon glyphicon-minus text-primary"></span> Retirer de la Playlist</span>\n' + '                                    </a>\n' + '                                </li>\n' + '                                <li>\n' + '                                    <a ng-click="isc.remove(item)"><span class="glyphicon glyphicon-remove text-danger"></span> Supprimer</a>\n' + '                                </li>\n' + '                                <li>\n' + '                                    <a ng-click="isc.reset(item)"><span class="glyphicon glyphicon-repeat"></span> Reset</a>\n' + '                                </li>\n' + '                            </ul>\n' + '                        </div>\n' + '                    </div>\n' + '                    <a class="overlay-main-button" target="_self" ng-href="{{ item.proxyURL  }}" >\n' + '                        <span ng-class="{\'glyphicon-globe\' : (!item.isDownloaded), \'glyphicon-play\' : (item.isDownloaded)}" class="glyphicon "></span>\n' + '                    </a>\n' + '                </div>\n' + '                <div class="text-center clearfix itemTitle center" >\n' + '                    <a ng-href="/podcasts/{{item.podcastId}}/item/{{item.id}}" tooltip-append-to-body="true" tooltip="{{ item.title }}" tooltip-placement="bottom" >\n' + '                        {{ item.title | characters:30 }}\n' + '                    </a>\n' + '                </div>\n' + '                <div class="text-center row-button">\n' + '                        <span ng-show="item.status == \'Started\' || item.status == \'Paused\'" >\n' + '                            <button ng-click="isc.toggleDownload(item)" type="button" class="btn btn-primary "><i class="glyphicon glyphicon-play"></i><i class="glyphicon glyphicon-pause"></i></button>\n' + '                            <button ng-click="isc.stopDownload(item)" type="button" class="btn btn-danger"><span class="glyphicon glyphicon-stop"></span></button>\n' + '                        </span>\n' + '\n' + '                    <button ng-click="item.download()" ng-show="(item.status != \'Started\' && item.status != \'Paused\' ) && !item.isDownloaded" type="button" class="btn btn-primary"><span class="glyphicon glyphicon-save"></span></button>\n' + '                    <a target="_self" ng-href="{{ item.proxyURL }}" ng-show="!item.isDownloaded" type="button" class="btn btn-info"><span class="glyphicon glyphicon-globe"></span></a>\n' + '\n' + '                    <a target="_self" ng-href="{{ item.proxyURL }}" ng-show="item.isDownloaded" type="button" class="btn btn-success"><span class="glyphicon glyphicon-play"></span></a>\n' + '                    <button ng-click="isc.remove(item)" ng-show="item.isDownloaded" type="button" class="btn btn-danger"><span class="glyphicon glyphicon-remove"></span></button>\n' + '                </div>\n' + '            </div>\n' + '        </div>\n' + '    </div>\n' + '    <!--</div>-->\n' + '    <div class="text-center row" ng-show="isc.totalPages > 1">\n' + '        <pagination items-per-page="12" max-size="10" boundary-links="true" total-items="isc.totalItems" ng-model="isc.currentPage" ng-change="isc.changePage()" class="pagination pagination-centered" previous-text="&lsaquo;" next-text="&rsaquo;" first-text="&laquo;" last-text="&raquo;"></pagination>\n' + '    </div>\n' + '</div>\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/navbar.html', '<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">\n' + '    <div class="container-fluid">\n' + '        <div class="navbar-header">\n' + '            <a class="navbar-brand" href="/items">Podcast Server</a>\n' + '            <ul class="nav navbar-nav pull-right" ng-transclude></ul>\n' + '        </div>\n' + '    </div>\n' + '</nav>');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/player.html', '<div class="container video-player">\n' + '    <br/>\n' + '    <div class="col-lg-8 player">\n' + '        <videogular vg-auto-play="pc.config.autoPlay" vg-player-ready="pc.onPlayerReady($API)" vg-complete="pc.onCompleteVideo()">\n' + '            <vg-media vg-src="pc.config.sources" vg-native-controls="false" vg-preload="pc.config.preload"></vg-media>\n' + '\n' + '            <vg-controls vg-autohide="pc.config.sources[0].type.indexOf(\'audio\') === -1 && pc.config.plugins.controls.autoHide" vg-autohide-time="pc.config.plugins.controls.autoHideTime">\n' + '                <vg-play-pause-button></vg-play-pause-button>\n' + '                <vg-time-display>{{ currentTime | date:\'mm:ss\' }}</vg-time-display>\n' + '                <vg-scrub-bar>\n' + '                    <vg-scrub-bar-current-time></vg-scrub-bar-current-time>\n' + '                </vg-scrub-bar>\n' + '                <vg-time-display>{{ timeLeft | date:\'mm:ss\' }}</vg-time-display>\n' + '                <vg-volume>\n' + '                    <vg-mute-button></vg-mute-button>\n' + '                    <vg-volume-bar></vg-volume-bar>\n' + '                </vg-volume>\n' + '                <vg-fullscreen-button ng-show="pc.config.sources[0].type.indexOf(\'audio\') === -1"></vg-fullscreen-button>\n' + '                <div class=\'btn-video-share\'><a target="_self" ng-href="{{ pc.config.sources[0].src }}" class="ionicons ion-android-share"></a></div>\n' + '            </vg-controls>\n' + '\n' + '            <vg-overlay-play></vg-overlay-play>\n' + '\n' + '            <vg-poster vg-url=\'pc.config.plugins.poster\'></vg-poster>\n' + '        </videogular>\n' + '    </div>\n' + '    <div class="playlist col-lg-4">\n' + '        <div class="row button-list">\n' + '            <div class="col-lg-6 col-sm-6 col-xs-6 col-md-6 text-center" ng-click="pc.reloadPlaylist()"><span class="ionicons ion-refresh"></span> Rafraichir</div>\n' + '            <div class="col-lg-6 col-sm-6 col-xs-6 col-md-6 text-center" ng-click="pc.removeAll ()"><span class="ionicons ion-trash-b"></span> Vider</div>\n' + '        </div>\n' + '        <div class="media clearfix"  ng-repeat="item in pc.playlist track by item.id" ng-class="{\'isReading\' : pc.currentVideo.id === item.id}">\n' + '\n' + '            <button ng-click="pc.remove(item)" type="button" class="pull-right close"><span aria-hidden="true">&times;</span></button>\n' + '\n' + '            <a class="pull-left cover" ng-click="pc.setVideo($index)">\n' + '                <img ng-src="{{item.cover.url}}" width="100" height="100" style="">\n' + '            </a>\n' + '\n' + '            <div class="media-body">\n' + '                <p ng-click="pc.setVideo($index)" class="">{{ item.title }}</p>\n' + '            </div>\n' + '        </div>\n' + '        \n' + '    </div>\n' + '\n' + '</div>');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-creation.html', '<div class="jumbotron">\n' + '    <div class="container">\n' + '        <h1>Ajouter un Podcast</h1>\n' + '    </div>\n' + '</div>\n' + '\n' + '<div class="container">\n' + '    <form class="form-horizontal" role="form" novalidate>\n' + '        <div class="form-group">\n' + '            <label for="title" class="col-sm-1 control-label">Titre</label>\n' + '\n' + '            <div class="col-sm-10">\n' + '                <input type="text" class="form-control" id="title" ng-model="pac.podcast.title" required placeholder="Titre">\n' + '            </div>\n' + '        </div>\n' + '        <div class="form-group">\n' + '            <label for="url" class="col-sm-1 control-label">URL</label>\n' + '\n' + '            <div class="col-sm-10">\n' + '                <input type="url" class="form-control" id="url" ng-model="pac.podcast.url" required placeholder="url" ng-change="pac.changeType();pac.findInfo();">\n' + '            </div>\n' + '        </div>\n' + '        <div class="form-group">\n' + '            <label for="url" class="col-sm-1 control-label">Tags</label>\n' + '            <div class="col-sm-10">\n' + '                <tags-input ng-model="pac.podcast.tags" display-property="name" min-length="1" class="bootstrap" placeholder="Ajouter un tag">\n' + '                    <auto-complete source="pac.loadTags($query)" min-length="2"></auto-complete>\n' + '                </tags-input>\n' + '            </div>\n' + '        </div>\n' + '        <div class="form-group">\n' + '            <label for="height" class="col-sm-1 control-label">Type</label>\n' + '\n' + '            <div class="col-sm-10">\n' + '                <select class="form-control" ng-model="pac.podcast.type">\n' + '                    <option ng-repeat="type in pac.types | orderBy: \'name\'" value="{{type.key}}">{{ type.name }}</option>\n' + '                </select>\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <div class="checkbox">\n' + '            <label>\n' + '                <input type="checkbox" ng-model="pac.podcast.hasToBeDeleted"> Suppression Automatique\n' + '            </label>\n' + '        </div>\n' + '\n' + '        <br/>\n' + '\n' + '        <div class="col-md-2 col-md-offset-1 text-center">\n' + '            <img ng-src="{{ pac.podcast.cover.url || \'http://placehold.it/200x200\' }}" class="img-thumbnail" width="200" height="200">\n' + '        </div>\n' + '        <div class="col-xs-12 col-md-9">\n' + '            <div class="form-group">\n' + '                <label for="url" class="col-sm-2 control-label">URL</label>\n' + '\n' + '                <div class="col-sm-9">\n' + '                    <input class="form-control" ng-model="pac.podcast.cover.url" required placeholder="url">\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="width" class="col-sm-2 control-label">Lageur</label>\n' + '\n' + '                <div class="col-sm-3">\n' + '                    <input type="number" class="form-control" id="width" ng-model="pac.podcast.cover.width" required>\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="height" class="col-sm-2 control-label">Hauteur</label>\n' + '\n' + '                <div class="col-sm-3">\n' + '                    <input type="number" class="form-control" id="height" ng-model="pac.podcast.cover.height" required>\n' + '                </div>\n' + '            </div>\n' + '        </div>\n' + '\n' + '\n' + '        <div class="form-group">\n' + '            <div class="col-sm-offset-2 col-sm-10">\n' + '                <button ng-click="pac.save()" class="btn btn-default">Sauvegarder</button>\n' + '            </div>\n' + '        </div>\n' + '    </form>\n' + '</div>\n' + '\n' + '\n' + '\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-detail.html', '\n' + '\n' + '<div class="container">\n' + '    <br/>\n' + '    <ol class="breadcrumb">\n' + '        <li><a href="/podcasts">Podcasts</a></li>\n' + '        <li><a class="active"> {{ pdc.podcast.title }}</a></li>\n' + '    </ol>\n' + '\n' + '    <div>\n' + '        <div class="jumbotron podcast-details-header" ng-style="{ \'background-image\' : \'url(\\\'\'+ pdc.podcast.cover.url + \'\\\')\'}">\n' + '            <div class="information-area">\n' + '                <div class="information-text">\n' + '                    <h3><strong>{{ pdc.podcast.title }}</strong></h3>\n' + '                    <p>{{ pdc.podcast.totalItems }} Episodes</p>\n' + '                </div>\n' + '                <div class="action-button pull-right">\n' + '                    <button ng-click="pdc.refresh()" type="button" class="btn btn-default"><span class="glyphicon glyphicon-refresh"></span></button>\n' + '                    <a type="button" class="btn btn-default" target="_self" ng-href="/api/podcast/{{ pdc.podcast.id }}/rss"><span class="ionicons ion-social-rss"></span></a>\n' + '                </div>\n' + '            </div>\n' + '        </div>\n' + '    </div>\n' + '<br/>\n' + '\n' + '<div class="col-md-12 col-xs-12 col-sm-12 col-lg-12">\n' + '\n' + '    <tabset>\n' + '        <tab heading="{{ pdc.podcastTabs[0].heading }}" active="pdc.podcastTabs[0].active" >\n' + '            <podcast-items-list podcast="pdc.podcast"></podcast-items-list>\n' + '        </tab>\n' + '        <tab heading="{{ pdc.podcastTabs[1].heading }}" active="pdc.podcastTabs[1].active" >\n' + '            <podcast-edition podcast="pdc.podcast"></podcast-edition>\n' + '        </tab>\n' + '        <tab heading="{{ pdc.podcastTabs[2].heading }}" ng-hide="pdc.podcastTabs[2].disabled" active="pdc.podcastTabs[2].active" disabled="pdc.podcastTabs[2].disabled">\n' + '            <podcast-upload podcast="pdc.podcast"></podcast-upload>\n' + '        </tab>\n' + '        <tab heading="{{ pdc.podcastTabs[3].heading }}" ng-hide="pdc.podcastTabs[3].disabled" active="pdc.podcastTabs[3].active" disabled="pdc.podcastTabs[3].disabled">\n' + '            <podcast-stats podcast="pdc.podcast"></podcast-stats>\n' + '        </tab>\n' + '    </tabset>\n' + '\n' + '\n' + '</div>\n' + '    </div>\n' + '\n' + '\n' + '\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-details-edition.html', '<br/>\n' + '<accordion close-others="true">\n' + '    <accordion-group heading="Podcast" is-open="true">\n' + '        <form class="form-horizontal" role="form">\n' + '            <div class="form-group">\n' + '                <label for="title" class="col-sm-2 control-label">Titre</label>\n' + '                <div class="col-sm-10">\n' + '                    <input type="text" class="form-control" id="title" ng-model="pec.podcast.title" required placeholder="Titre">\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="url" class="col-sm-2 control-label">URL</label>\n' + '                <div class="col-sm-10">\n' + '                    <input type="url" class="form-control" id="url" ng-model="pec.podcast.url" required placeholder="url">\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <div class="checkbox col-sm-offset-3">\n' + '                    <label>\n' + '                        <input type="checkbox" ng-model="pec.podcast.hasToBeDeleted"> Suppression Auto\n' + '                    </label>\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="url" class="col-sm-2 control-label">Tags</label>\n' + '                <div class="col-sm-10">\n' + '                    <tags-input ng-model="pec.podcast.tags" display-property="name" min-length="1" class="bootstrap" placeholder="Ajouter un tag">\n' + '                        <auto-complete source="pec.loadTags($query)" min-length="2"></auto-complete>\n' + '                    </tags-input>\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="height" class="col-sm-2 control-label" >Type</label>\n' + '                <div class="col-sm-10" >\n' + '                    <select class="form-control" ng-model="pec.podcast.type">\n' + '                        <option value="BeInSports">Be In Sports</option>\n' + '                        <option value="CanalPlus">Canal+</option>\n' + '                        <option value="JeuxVideoCom">Jeux Video Com</option>\n' + '                        <option value="JeuxVideoFR">Jeux Video Fr</option>\n' + '                        <option value="Parleys">Parleys</option>\n' + '                        <option value="Pluzz">Pluzz</option>\n' + '                        <option value="RSS">RSS</option>\n' + '                        <option value="send">Send</option>\n' + '                        <option value="Youtube">Youtube</option>\n' + '                    </select>\n' + '                </div>\n' + '            </div>\n' + '\n' + '            <div class="form-group">\n' + '                <div class="col-sm-offset-2 col-sm-10">\n' + '                    <button ng-click="pec.save()" class="btn btn-default">Sauvegarder</button>\n' + '                </div>\n' + '            </div>\n' + '        </form>\n' + '    </accordion-group>\n' + '    <accordion-group heading="Cover">\n' + '        <form class="form-horizontal" role="form">\n' + '            <div class="form-group">\n' + '                <label for="url" class="col-sm-2 control-label">URL</label>\n' + '                <div class="col-sm-10">\n' + '                    <input type="url" class="form-control" id="url" ng-model="pec.podcast.cover.url" required placeholder="url">\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="width" class="col-sm-2 control-label">Lageur</label>\n' + '                <div class="col-sm-10">\n' + '                    <input type="number" class="form-control" id="width" ng-model="pec.podcast.cover.width" required placeholder="url">\n' + '                </div>\n' + '            </div>\n' + '            <div class="form-group">\n' + '                <label for="height" class="col-sm-2 control-label">Hauteur</label>\n' + '                <div class="col-sm-10">\n' + '                    <input type="number" class="form-control" id="height" ng-model="pec.podcast.cover.height" required placeholder="url">\n' + '                </div>\n' + '            </div>\n' + '\n' + '            <div class="form-group">\n' + '                <div class="col-sm-offset-2 col-sm-10">\n' + '                    <button ng-click="pec.save()" class="btn btn-default">Sauvegarder</button>\n' + '                </div>\n' + '            </div>\n' + '        </form>\n' + '    </accordion-group>\n' + '    <accordion-group heading="Actions">\n' + '        <button type="button" class="btn btn-warning" ng-click="pec.deletePodcast()">\n' + '            <span class="glyphicon glyphicon-trash"></span> Delete\n' + '        </button>\n' + '    </accordion-group>\n' + '</accordion>\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-details-episodes.html', '<br/>\n' + '<div ng-swipe-right="pic.swipePage(-1)" ng-swipe-left="pic.swipePage(1)">\n' + '    <div class="media clearfix"  ng-repeat="item in pic.podcast.items | orderBy:\'-pubdate\' track by item.id">\n' + '        <div class="buttonList pull-right">\n' + '            <!-- T\xE9l\xE9chargement en cours -->\n' + '        <span ng-show="item.status == \'Started\' || item.status == \'Paused\'" >\n' + '            <button ng-click="pic.toggleDownload(item)" type="button" class="btn btn-primary "><i class="glyphicon glyphicon-play"></i><i class="glyphicon glyphicon-pause"></i></button>\n' + '            <button ng-click="pic.stopDownload(item)" type="button" class="btn btn-danger"><span class="glyphicon glyphicon-stop"></span></button>\n' + '        </span>\n' + '\n' + '            <!-- Lancer le t\xE9l\xE9chargement -->\n' + '            <button ng-click="item.download()" ng-show="(item.status != \'Started\' && item.status != \'Paused\' ) && !item.isDownloaded" type="button" class="btn btn-primary"><span class="glyphicon glyphicon-save"></span></button>\n' + '\n' + '            <!-- Lire dans le player -->\n' + '            <a ng-href="/podcasts/{{ item.podcastId }}/item/{{ item.id }}/play" ng-show="item.isDownloaded" type="button" class="btn btn-success"><span class="ionicons ion-social-youtube"></span></a>\n' + '            \n' + '            <!-- Supprimer l\'item -->\n' + '            <button ng-click="pic.remove(item)" ng-show="(item.status != \'Started\' && item.status != \'Paused\' )" type="button" class="btn btn-danger"><span class="glyphicon glyphicon-remove"></span></button>\n' + '\n' + '            <!-- Menu compl\xE9mentaire -->\n' + '            <div class="btn-group" dropdown is-open="isopen">\n' + '                <button type="button" class="btn btn-default dropdown-toggle" dropdown-toggle><i class="ionicons ion-android-more"></i></button>\n' + '                <ul class="dropdown-menu dropdown-menu-right" role="menu">\n' + '                    <li ng-show="item.isDownloaded"><a target="_self" ng-href="{{ item.proxyURL }}"><span class="glyphicon glyphicon-play text-success"></span> Lire</a></li>\n' + '                    <li ng-show="item.isDownloaded">\n' + '                        <a ng-hide="pic.isInPlaylist(item)" ng-click="pic.addOrRemoveInPlaylist(item)">\n' + '                            <span class="glyphicon glyphicon-plus text-primary"></span> Ajouter \xE0 la Playlist\n' + '                        </a>\n' + '                        <a ng-show="pic.isInPlaylist(item)" ng-click="pic.addOrRemoveInPlaylist(item)">\n' + '                            <span class="glyphicon glyphicon-minus text-primary"></span> Retirer de la Playlist\n' + '                        </a>\n' + '                    </li>\n' + '                    <li><a target="_self" ng-href="{{ item.url }}"><span class="glyphicon glyphicon-globe text-info"></span> Lire en ligne</a></li>\n' + '                    <li><a ng-click="pic.reset(item)"><span class="glyphicon glyphicon-repeat"></span> Reset</a></li>\n' + '                </ul>\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <a class="pull-left" ng-href="/podcasts/{{pic.podcast.id}}/item/{{item.id}}">\n' + '            <img ng-src="{{item.cover.url}}" width="100" height="100" style="">\n' + '        </a>\n' + '        \n' + '        <div class="media-body">\n' + '            <h4 class="media-heading">{{ item.title }}</h4>\n' + '            <p class="description hidden-xs hidden-sm branch-name">{{item.description | htmlToPlaintext | characters : 130 }}</p>\n' + '            <p><strong>{{item.pubdate | date : \'dd/MM/yyyy \xE0 HH:mm\' }}</strong></p>\n' + '        </div>\n' + '    </div>\n' + '\n' + '    <div ng-show="pic.podcast.totalItems > pic.itemPerPage" class="text-center">\n' + '        <pagination items-per-page="pic.itemPerPage" max-size="10" boundary-links="true" total-items="pic.podcast.totalItems" ng-model="pic.currentPage" ng-change="pic.loadPage()" class="pagination pagination-centered" previous-text="&lsaquo;" next-text="&rsaquo;" first-text="&laquo;" last-text="&raquo;"></pagination>\n' + '    </div>\n' + '</div>\n' + '\n' + '        ');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-details-stats.html', '<br/>\n' + '<div>\n' + '    <div class="input-group col-xs-offset-5 col-sm-offset-5 col-md-offset-5 col-lg-offset-5 col-md-2 col-lg-2 col-xs-2 col-sm-2">\n' + '        <a class="input-group-addon" ng-click="pdsc.navigate(-1)"> - </a>\n' + '        <input type="number" class="form-control text-center" placeholder="Number of Month" ng-model="pdsc.month" ng-change="pdsc.generateChartData()" ng-model-options="{ debounce: 300 }">\n' + '        <a class="input-group-addon" ng-click="pdsc.navigate(1)"> + </a>\n' + '    </div>\n' + '</div>\n' + '<div class="row">\n' + '    <highchart id="statsOfPodcast" config="pdsc.chartConfig"></highchart>\n' + '</div>\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcast-details-upload.html', '<br/>\n' + '<div class="upload-item">\n' + '    <div class="drop-box"\n' + '         ng-file-drop="onFileSelect($files)"\n' + '         ng-file-drag-over-class="dropping"\n' + '         ng-file-drag-over-delay="100">\n' + '        <div class="text-center clearfix upload-text">\n' + '            D\xE9poser un ou des fichiers ici\n' + '        </div>\n' + '    </div>\n' + '</div>\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/podcasts-list.html', '<div class="container podcastlist">\n' + '\n' + '    <div class="form-inline search-bar row">\n' + '        <div class="form-group">\n' + '            <div class="input-group">\n' + '                <div class="input-group-addon"><i class="glyphicon glyphicon-search"></i></div>\n' + '                <input type="text" class="form-control" ng-model="plc.filters.title" placeholder="Recherche globale" ng-model-options="{ debounce: 500 }">\n' + '            </div>\n' + '        </div>\n' + '\n' + '        <div class="form-group">\n' + '            <!-- glyphicon glyphicon-download -->\n' + '            <div class="input-group">\n' + '                <div class="input-group-addon"><i class="glyphicon glyphicon-download"></i></div>\n' + '                <select class="form-control" ng-model="plc.filters.type">\n' + '                    <option value>All</option>\n' + '                    <option value="BeInSports">Be In Sports</option>\n' + '                    <option value="CanalPlus">Canal+</option>\n' + '                    <option value="JeuxVideoCom">Jeux Video Com</option>\n' + '                    <option value="JeuxVideoFR">Jeux Video Fr</option>\n' + '                    <option value="Parleys">Parleys</option>\n' + '                    <option value="Pluzz">Pluzz</option>\n' + '                    <option value="RSS">RSS</option>\n' + '                    <option value="send">Send</option>\n' + '                    <option value="Youtube">Youtube</option>\n' + '                </select>\n' + '            </div>\n' + '        </div>\n' + '\n' + '    </div>\n' + '\n' + '\n' + '    <div class="row">\n' + '        <div class="col-lg-2 col-md-3 col-sm-4 col-xs-6 thumb" ng-repeat="podcast in plc.podcasts | filter: plc.filters | orderBy:\'-lastUpdate\'">\n' + '            <a ng-href="/podcasts/{{ ::podcast.id }}" >\n' + '                <img    class="img-responsive img-rounded" ng-src="{{ ::podcast.cover.url}}" width="{{ ::podcast.cover.width }}" height="{{ ::podcast.cover.height }}"\n' + '                        notooltip-append-to-body="true" tooltip-placement="bottom" tooltip="{{ ::podcast.title }}"\n' + '                        />\n' + '            </a>\n' + '        </div>\n' + '    </div>\n' + '</div>\n' + '\n' + '');
            }
        ]);
    }());
    (function (module) {
        try {
            module = angular.module('ps.partial');
        } catch (e) {
            module = angular.module('ps.partial', []);
        }
        module.run([
            '$templateCache',
            function ($templateCache) {
                $templateCache.put('html/stats.html', '<div class="container">\n' + '    <br/>\n' + '    <div>\n' + '        <div class="input-group col-xs-offset-5 col-sm-offset-5 col-md-offset-5 col-lg-offset-5 col-md-2 col-lg-2 col-xs-2 col-sm-2">\n' + '            <a class="input-group-addon" ng-click="sc.navigate(-1)"> - </a>\n' + '            <input type="number" class="form-control text-center" placeholder="Number of Month" ng-model="sc.month" ng-change="sc.generateChartData()" ng-model-options="{ debounce: 300 }">\n' + '            <a class="input-group-addon" ng-click="sc.navigate(1)"> + </a>\n' + '        </div>\n' + '    </div>\n' + '    <div class="row">\n' + '        <highchart id="statsByType" config="sc.chartConfig"></highchart>\n' + '    </div>\n' + '</div>');
            }
        ]);
    }());
    var DownloadCtrl = function () {
        function DownloadCtrl($scope, DonwloadManager, $notification) {
            var _this2 = this;
            _classCallCheck(this, DownloadCtrl);
            this.DonwloadManager = DonwloadManager;
            this.$notification = $notification;
            this.items = [];
            this.waitingitems = [];
            this.numberOfSimDl = 0;
            this.DonwloadManager.getNumberOfSimDl().then(function (value) {
                _this2.numberOfSimDl = parseInt(value);
            });
            /** Websocket Connection */
            this.DonwloadManager.ws.subscribe('/app/download', function (message) {
                return _this2.onSubscribeDownload(message);
            }, $scope).subscribe('/app/waiting', function (message) {
                return _this2.onSubscribeWaiting(message);
            }, $scope).subscribe('/topic/download', function (message) {
                return _this2.onDownloadUpdate(message);
            }, $scope).subscribe('/topic/waiting', function (message) {
                return _this2.onWaitingUpdate(message);
            }, $scope);
        }
        DownloadCtrl.$inject = ["$scope", "DonwloadManager", "$notification"];
        _createClass(DownloadCtrl, [
            {
                key: 'onSubscribeDownload',
                value: function onSubscribeDownload(message) {
                    this.items = JSON.parse(message.body);
                }
            },
            {
                key: 'onSubscribeWaiting',
                value: function onSubscribeWaiting(message) {
                    this.waitingitems = JSON.parse(message.body);
                }
            },
            {
                key: 'onDownloadUpdate',
                value: function onDownloadUpdate(message) {
                    var item = JSON.parse(message.body);
                    var elemToUpdate = _.find(this.items, { id: item.id });
                    switch (item.status) {
                    case 'Started':
                    case 'Paused':
                        if (elemToUpdate)
                            _.assign(elemToUpdate, item);
                        else
                            this.items.push(item);
                        break;
                    case 'Finish':
                        try {
                            this.$notification('T\xE9l\xE9chargement termin\xE9', {
                                body: item.title,
                                icon: item.cover.url,
                                delay: 5000
                            });
                        } catch (e) {
                        }
                        this.onStoppedFromWS(elemToUpdate);
                        break;
                    case 'Stopped':
                        this.onStoppedFromWS(elemToUpdate);
                        break;
                    }
                }
            },
            {
                key: 'onStoppedFromWS',
                value: function onStoppedFromWS(elemToUpdate) {
                    if (elemToUpdate) {
                        _.remove(this.items, function (item) {
                            return item.id === elemToUpdate.id;
                        });
                    }
                }
            },
            {
                key: 'onWaitingUpdate',
                value: function onWaitingUpdate(message) {
                    var remoteWaitingItems = JSON.parse(message.body);
                    _.updateinplace(this.waitingitems, remoteWaitingItems, function (inArray, elem) {
                        return _.findIndex(inArray, { id: elem.id });
                    }, true);
                }
            },
            {
                key: 'getTypeFromStatus',
                value: function getTypeFromStatus(item) {
                    if (item.status === 'Paused') {
                        return 'warning';
                    }
                    return 'info';
                }
            },
            {
                key: 'updateNumberOfSimDl',
                value: function updateNumberOfSimDl(number) {
                    this.DonwloadManager.updateNumberOfSimDl(number);
                }
            },
            {
                key: 'download',
                /** Spécifique aux éléments de la liste : **/
                value: function download(item) {
                    this.DonwloadManager.download(item);
                }
            },
            {
                key: 'stopDownload',
                value: function stopDownload(item) {
                    this.DonwloadManager.ws.stop(item);
                }
            },
            {
                key: 'toggleDownload',
                value: function toggleDownload(item) {
                    this.DonwloadManager.ws.toggle(item);
                }
            },
            {
                key: 'stopAllDownload',
                /** Global **/
                value: function stopAllDownload() {
                    this.DonwloadManager.stopAllDownload();
                }
            },
            {
                key: 'pauseAllDownload',
                value: function pauseAllDownload() {
                    this.DonwloadManager.pauseAllDownload();
                }
            },
            {
                key: 'restartAllCurrentDownload',
                value: function restartAllCurrentDownload() {
                    this.DonwloadManager.restartAllCurrentDownload();
                }
            },
            {
                key: 'removeFromQueue',
                value: function removeFromQueue(item) {
                    this.DonwloadManager.removeFromQueue(item);
                }
            },
            {
                key: 'dontDonwload',
                value: function dontDonwload(item) {
                    this.DonwloadManager.dontDonwload(item);
                }
            },
            {
                key: 'moveInWaitingList',
                value: function moveInWaitingList(item, position) {
                    this.DonwloadManager.moveInWaitingList(item, position);
                }
            }
        ]);
        return DownloadCtrl;
    }();
    angular.module('ps.download', [
        'ps.config.route',
        'ps.dataService.donwloadManager',
        'notification'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        return $routeProvider.when('/download', {
            templateUrl: 'html/download.html',
            controller: 'DownloadCtrl',
            controllerAs: 'dc',
            hotkeys: commonKey
        });
    }]).controller('DownloadCtrl', DownloadCtrl);
    var ItemDetailCtrl = function () {
        function ItemDetailCtrl($scope, DonwloadManager, $location, playlistService, podcast, item) {
            var _this3 = this;
            _classCallCheck(this, ItemDetailCtrl);
            this.item = item;
            this.$location = $location;
            this.item.podcast = podcast;
            this.playlistService = playlistService;
            this.DonwloadManager = DonwloadManager;
            //** WebSocket Inscription **//
            var webSockedUrl = '/topic/podcast/'.concat(this.item.podcast.id);
            this.DonwloadManager.ws.subscribe(webSockedUrl, function (message) {
                var itemFromWS = JSON.parse(message.body);
                if (itemFromWS.id == _this3.item.id) {
                    _.assign(_this3.item, itemFromWS);
                }
            }, $scope);
        }
        ItemDetailCtrl.$inject = ["$scope", "DonwloadManager", "$location", "playlistService", "podcast", "item"];
        _createClass(ItemDetailCtrl, [
            {
                key: 'stopDownload',
                value: function stopDownload(item) {
                    this.DonwloadManager.ws.stop(item);
                }
            },
            {
                key: 'toggleDownload',
                value: function toggleDownload(item) {
                    this.DonwloadManager.ws.toggle(item);
                }
            },
            {
                key: 'remove',
                value: function remove(item) {
                    var _this4 = this;
                    return item.remove().then(function () {
                        _this4.playlistService.remove(item);
                        _this4.$location.path('/podcasts/'.concat(_this4.item.podcast.id));
                    });
                }
            },
            {
                key: 'reset',
                value: function reset(item) {
                    var _this5 = this;
                    return item.reset().then(function (itemReseted) {
                        _.assign(_this5.item, itemReseted);
                        _this5.playlistService.remove(item);
                    });
                }
            },
            {
                key: 'toggleInPlaylist',
                value: function toggleInPlaylist() {
                    this.playlistService.addOrRemove(this.item);
                }
            },
            {
                key: 'isInPlaylist',
                value: function isInPlaylist() {
                    return this.playlistService.contains(this.item);
                }
            }
        ]);
        return ItemDetailCtrl;
    }();
    angular.module('ps.item.details', [
        'ps.dataService.donwloadManager',
        'ps.player'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        $routeProvider.when('/podcasts/:podcastId/item/:itemId', {
            templateUrl: 'html/item-detail.html',
            controller: 'ItemDetailCtrl',
            controllerAs: 'idc',
            hotkeys: commonKey,
            resolve: {
                item: ["itemService", "$route", function item(itemService, $route) {
                    return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);
                }],
                podcast: ["podcastService", "$route", function podcast(podcastService, $route) {
                    return podcastService.findById($route.current.params.podcastId);
                }]
            }
        });
    }]).controller('ItemDetailCtrl', ItemDetailCtrl);
    /**
 * Created by kevin on 01/11/14.
 */
    angular.module('ps.item', [
        'ps.item.details',
        'ps.item.player'
    ]);
    var ItemPlayerController = function ItemPlayerController(podcast, item, $timeout, deviceDetectorService) {
        _classCallCheck(this, ItemPlayerController);
        this.item = item;
        this.item.podcast = podcast;
        this.$timeout = $timeout;
        this.config = {
            autoPlay: true,
            sources: [{
                    src: this.item.proxyURL,
                    type: this.item.mimeType
                }],
            plugins: {
                controls: {
                    autoHide: !deviceDetectorService.isTouchedDevice(),
                    autoHideTime: 2000
                },
                poster: this.item.cover.url
            }
        };
    };
    ItemPlayerController.$inject = ["podcast", "item", "$timeout", "deviceDetectorService"];
    angular.module('ps.item.player', [
        'ngSanitize',
        'ngRoute',
        'device-detection',
        'com.2fdevs.videogular',
        'com.2fdevs.videogular.plugins.poster',
        'com.2fdevs.videogular.plugins.controls',
        'com.2fdevs.videogular.plugins.overlayplay',
        'com.2fdevs.videogular.plugins.buffering'
    ]).config(["$routeProvider", function ($routeProvider) {
        $routeProvider.when('/podcasts/:podcastId/item/:itemId/play', {
            templateUrl: 'html/item-player.html',
            controller: 'ItemPlayerController',
            controllerAs: 'ipc',
            resolve: {
                item: ["itemService", "$route", function item(itemService, $route) {
                    return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);
                }],
                podcast: ["podcastService", "$route", function podcast(podcastService, $route) {
                    return podcastService.findById($route.current.params.podcastId);
                }]
            }
        });
    }]).controller('ItemPlayerController', ItemPlayerController);
    var PlayerController = function () {
        function PlayerController(playlistService, $timeout, deviceDetectorService) {
            _classCallCheck(this, PlayerController);
            this.playlistService = playlistService;
            this.$timeout = $timeout;
            this.playlist = [];
            this.state = null;
            this.API = null;
            this.currentVideo = {};
            this.config = {
                autoPlay: true,
                sources: [],
                plugins: {
                    controls: {
                        autoHide: !deviceDetectorService.isTouchedDevice(),
                        autoHideTime: 2000
                    },
                    poster: ''
                }
            };
            this.reloadPlaylist();
        }
        PlayerController.$inject = ["playlistService", "$timeout", "deviceDetectorService"];
        _createClass(PlayerController, [
            {
                key: 'onPlayerReady',
                value: function onPlayerReady(API) {
                    this.API = API;
                    if (this.API.currentState == 'play' || this.isCompleted)
                        this.API.play();
                    this.isCompleted = false;
                    this.setVideo(0);
                }
            },
            {
                key: 'onCompleteVideo',
                value: function onCompleteVideo() {
                    var indexOfVideo = this.getIndexOfVideoInPlaylist(this.currentVideo);
                    this.isCompleted = true;
                    if (indexOfVideo + 1 === this.playlist.length) {
                        this.currentVideo = this.playlist[0];
                        return;
                    }
                    this.setVideo(indexOfVideo + 1);
                }
            },
            {
                key: 'reloadPlaylist',
                value: function reloadPlaylist() {
                    _.updateinplace(this.playlist, this.playlistService.playlist(), function (inArray, elem) {
                        return _.findIndex(inArray, { id: elem.id });
                    });
                }
            },
            {
                key: 'setVideo',
                value: function setVideo(index) {
                    this.currentVideo = this.playlist[index];
                    if (this.currentVideo !== null && this.currentVideo !== undefined) {
                        this.API.stop();
                        this.config.sources = [{
                                src: this.currentVideo.proxyURL,
                                type: this.currentVideo.mimeType
                            }];
                        this.config.plugins.poster = this.currentVideo.cover.url;
                    }
                }
            },
            {
                key: 'remove',
                value: function remove(item) {
                    this.playlistService.remove(item);
                    this.reloadPlaylist();
                    if (this.config.sources.length > 0 && this.config.sources[0].src === item.proxyURL) {
                        this.setVideo(0);
                    }
                }
            },
            {
                key: 'removeAll',
                value: function removeAll() {
                    this.playlistService.removeAll();
                    this.reloadPlaylist();
                }
            },
            {
                key: 'getIndexOfVideoInPlaylist',
                value: function getIndexOfVideoInPlaylist(item) {
                    return this.playlist.indexOf(item);
                }
            }
        ]);
        return PlayerController;
    }();
    angular.module('ps.player', [
        'ngSanitize',
        'ngRoute',
        'device-detection',
        'com.2fdevs.videogular',
        'com.2fdevs.videogular.plugins.poster',
        'com.2fdevs.videogular.plugins.controls',
        'com.2fdevs.videogular.plugins.overlayplay',
        'com.2fdevs.videogular.plugins.buffering',
        'ps.player.playlist'
    ]).config(["$routeProvider", function ($routeProvider) {
        $routeProvider.when('/player', {
            templateUrl: 'html/player.html',
            controller: 'PlayerController',
            controllerAs: 'pc'
        });
    }]).controller('PlayerController', PlayerController);
    var PlaylistService = function () {
        function PlaylistService($localStorage) {
            _classCallCheck(this, PlaylistService);
            this.$localStorage = $localStorage;
            this.$localStorage.playlist = this.$localStorage.playlist || [];
        }
        PlaylistService.$inject = ["$localStorage"];
        _createClass(PlaylistService, [
            {
                key: 'playlist',
                value: function playlist() {
                    return this.$localStorage.playlist;
                }
            },
            {
                key: 'add',
                value: function add(item) {
                    this.$localStorage.playlist.push(item);
                }
            },
            {
                key: 'remove',
                value: function remove(item) {
                    this.$localStorage.playlist = _.remove(this.$localStorage.playlist, function (elem) {
                        return elem.id !== item.id;
                    });
                }
            },
            {
                key: 'contains',
                value: function contains(item) {
                    return angular.isObject(_.find(this.$localStorage.playlist, { id: item.id }));
                }
            },
            {
                key: 'addOrRemove',
                value: function addOrRemove(item) {
                    this.contains(item) ? this.remove(item) : this.add(item);
                }
            },
            {
                key: 'removeAll',
                value: function removeAll() {
                    this.$localStorage.playlist = [];
                }
            }
        ]);
        return PlaylistService;
    }();
    angular.module('ps.player.playlist', ['ngStorage']).service('playlistService', PlaylistService);
    var PodcastCreationController = function () {
        function PodcastCreationController($location, defaultPodcast, tagService, podcastService, types) {
            _classCallCheck(this, PodcastCreationController);
            this.podcastService = podcastService;
            this.$location = $location;
            this.tagService = tagService;
            this.podcast = angular.extend(this.podcastService.getNewPodcast(), defaultPodcast);
            this.types = types;
        }
        PodcastCreationController.$inject = ["$location", "defaultPodcast", "tagService", "podcastService", "types"];
        _createClass(PodcastCreationController, [
            {
                key: 'findInfo',
                value: function findInfo() {
                    var _this6 = this;
                    return this.podcastService.findInfo(this.podcast.url).then(function (podcastFetched) {
                        _this6.podcast.title = podcastFetched.title;
                        _this6.podcast.description = podcastFetched.description;
                        _this6.podcast.type = podcastFetched.type;
                        _this6.podcast.cover.url = podcastFetched.cover.url;
                    });
                }
            },
            {
                key: 'loadTags',
                value: function loadTags(query) {
                    return this.tagService.search(query);
                }
            },
            {
                key: 'changeType',
                value: function changeType() {
                    if (/beinsports\.fr/i.test(this.podcast.url)) {
                        this.podcast.type = 'BeInSports';
                    } else if (/canalplus\.fr/i.test(this.podcast.url)) {
                        this.podcast.type = 'CanalPlus';
                    } else if (/jeuxvideo\.fr/i.test(this.podcast.url)) {
                        this.podcast.type = 'JeuxVideoFR';
                    } else if (/jeuxvideo\.com/i.test(this.podcast.url)) {
                        this.podcast.type = 'JeuxVideoCom';
                    } else if (/parleys\.com/i.test(this.podcast.url)) {
                        this.podcast.type = 'Parleys';
                    } else if (/pluzz\.francetv\.fr/i.test(this.podcast.url)) {
                        this.podcast.type = 'Pluzz';
                    } else if (/youtube\.com/i.test(this.podcast.url)) {
                        this.podcast.type = 'Youtube';
                    } else if (this.podcast.url.length > 0) {
                        this.podcast.type = 'RSS';
                    } else {
                        this.podcast.type = 'Send';
                    }
                }
            },
            {
                key: 'save',
                value: function save() {
                    var _this7 = this;
                    this.podcastService.save(this.podcast).then(function (podcast) {
                        return _this7.$location.path('/podcasts/' + podcast.id);
                    });
                }
            }
        ]);
        return PodcastCreationController;
    }();
    angular.module('ps.podcast.creation', [
        'ps.config.route',
        'ps.dataservice',
        'ngTagsInput'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        $routeProvider.when('/podcast-creation', {
            templateUrl: 'html/podcast-creation.html',
            controller: 'PodcastAddCtrl',
            controllerAs: 'pac',
            hotkeys: commonKey,
            resolve: {
                types: ["typeService", function types(typeService) {
                    return typeService.findAll();
                }]
            }
        });
    }]).constant('defaultPodcast', {
        hasToBeDeleted: true,
        cover: {
            height: 200,
            width: 200
        }
    }).controller('PodcastAddCtrl', PodcastCreationController);
    var PodcastsListCtrl = function PodcastsListCtrl(podcasts) {
        _classCallCheck(this, PodcastsListCtrl);
        this.podcasts = podcasts;
        this.filters = {
            title: '',
            type: ''
        };
    };
    PodcastsListCtrl.$inject = ["podcasts"];
    angular.module('ps.podcast.list', [
        'ps.config.route',
        'ps.dataService.podcast'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        $routeProvider.when('/podcasts', {
            templateUrl: 'html/podcasts-list.html',
            controller: 'PodcastsListCtrl',
            controllerAs: 'plc',
            hotkeys: commonKey,
            resolve: {
                podcasts: ["podcastService", function podcasts(podcastService) {
                    return podcastService.findAll();
                }]
            }
        });
    }]).controller('PodcastsListCtrl', PodcastsListCtrl);
    /**
 * Created by kevin on 01/11/14.
 */
    angular.module('ps.podcast', [
        'ps.podcast.details',
        'ps.podcast.creation',
        'ps.podcast.list'
    ]);
    var SearchItemCache = function () {
        function SearchItemCache(DefaultItemSearchParameters, $sessionStorage) {
            _classCallCheck(this, SearchItemCache);
            this.$sessionStorage = $sessionStorage;
            this.$sessionStorage.searchParameters = DefaultItemSearchParameters;
        }
        SearchItemCache.$inject = ["DefaultItemSearchParameters", "$sessionStorage"];
        _createClass(SearchItemCache, [
            {
                key: 'getParameters',
                value: function getParameters() {
                    return this.$sessionStorage.searchParameters;
                }
            },
            {
                key: 'page',
                value: function page(pageNumber) {
                    if (angular.isNumber(pageNumber)) {
                        this.$sessionStorage.searchParameters.page = pageNumber;
                    }
                    return this.$sessionStorage.searchParameters.page;
                }
            },
            {
                key: 'size',
                value: function size(sizeNumber) {
                    if (angular.isNumber(sizeNumber)) {
                        this.$sessionStorage.searchParameters.size = sizeNumber;
                    }
                    return this.$sessionStorage.searchParameters.size;
                }
            },
            {
                key: 'updateSearchParam',
                value: function updateSearchParam(searchParam) {
                    this.$sessionStorage.searchParameters.term = searchParam.term;
                    this.$sessionStorage.searchParameters.tags = searchParam.tags;
                    this.$sessionStorage.searchParameters.direction = searchParam.direction;
                    this.$sessionStorage.searchParameters.properties = searchParam.properties;
                }
            }
        ]);
        return SearchItemCache;
    }();
    var ItemSearchCtrl = function () {
        function ItemSearchCtrl($scope, SearchItemCache, $location, itemService, tagService, DonwloadManager, playlistService, items) {
            var _this8 = this;
            _classCallCheck(this, ItemSearchCtrl);
            /* DI */
            this.$location = $location;
            this.itemService = itemService;
            this.tagService = tagService;
            this.DownloadManager = DonwloadManager;
            this.playlistService = playlistService;
            this.SearchItemCache = SearchItemCache;
            /* Constructor Init */
            this.totalItems = Number.MAX_VALUE;
            this.maxSize = 10;
            this.currentPage = this.SearchItemCache.page() + 1;
            this.searchParameters = this.SearchItemCache.getParameters();
            //** WebSocket Subscription **//
            this.DownloadManager.ws.subscribe('/topic/download', function (message) {
                return _this8.updateItemFromWS(message);
            }, $scope);
            $scope.$on('$routeUpdate', function () {
                if (_this8.currentPage !== _this8.$location.search().page) {
                    _this8.currentPage = _this8.$location.search().page || 1;
                    _this8.changePage();
                }
            });
            /*this.changePage();*/
            this.attachResponse(items);
        }
        ItemSearchCtrl.$inject = ["$scope", "SearchItemCache", "$location", "itemService", "tagService", "DonwloadManager", "playlistService", "items"];
        _createClass(ItemSearchCtrl, [
            {
                key: 'updateItemFromWS',
                value: function updateItemFromWS(wsMessage) {
                    var item = JSON.parse(wsMessage.body);
                    var elemToUpdate = _.find(this.items, { id: item.id });
                    if (elemToUpdate)
                        _.assign(elemToUpdate, item);
                }
            },
            {
                key: 'changePage',
                value: function changePage() {
                    var _this9 = this;
                    this.SearchItemCache.page(this.calculatePage());
                    return this.itemService.search(this.SearchItemCache.getParameters()).then(function (itemsResponse) {
                        return _this9.attachResponse(itemsResponse);
                    });
                }
            },
            {
                key: 'attachResponse',
                value: function attachResponse(itemsResponse) {
                    this.items = itemsResponse.content;
                    this.totalPages = itemsResponse.totalPages;
                    this.totalItems = itemsResponse.totalElements;
                    this.currentPage = this.SearchItemCache.page(itemsResponse.number) + 1;
                    this.$location.search('page', this.currentPage);
                }
            },
            {
                key: 'swipePage',
                value: function swipePage(val) {
                    this.currentPage = this.SearchItemCache.page() + val + 1;
                    return this.changePage();
                }
            },
            {
                key: 'remove',
                //** Item Operation **//
                value: function remove(item) {
                    var _this10 = this;
                    return item.remove().then(function () {
                        return _this10.playlistService.remove(item);
                    }).then(function () {
                        return _this10.changePage();
                    });
                }
            },
            {
                key: 'reset',
                value: function reset(item) {
                    var _this11 = this;
                    return item.reset().then(function (itemReseted) {
                        var itemInList = _.find(_this11.items, { id: itemReseted.id });
                        _.assign(itemInList, itemReseted);
                        return itemInList;
                    }).then(function (itemInList) {
                        return _this11.playlistService.remove(itemInList);
                    });
                }
            },
            {
                key: 'stopDownload',
                value: function stopDownload(item) {
                    this.DownloadManager.ws.stop(item);
                }
            },
            {
                key: 'toggleDownload',
                value: function toggleDownload(item) {
                    return this.DownloadManager.ws.toggle(item);
                }
            },
            {
                key: 'loadTags',
                value: function loadTags(query) {
                    return this.tagService.search(query);
                }
            },
            {
                key: 'addOrRemove',
                //** Playlist Manager **//
                value: function addOrRemove(item) {
                    return this.playlistService.addOrRemove(item);
                }
            },
            {
                key: 'isInPlaylist',
                value: function isInPlaylist(item) {
                    return this.playlistService.contains(item);
                }
            },
            {
                key: 'calculatePage',
                value: function calculatePage() {
                    if (this.currentPage <= 1) {
                        return 0;
                    } else if (this.currentPage > Math.ceil(this.totalItems / this.SearchItemCache.size())) {
                        return Math.ceil(this.totalItems / this.SearchItemCache.size()) - 1;
                    } else {
                        return this.currentPage - 1;
                    }
                }
            },
            {
                key: 'resetSearch',
                value: function resetSearch() {
                    this.currentPage = 1;
                    this.SearchItemCache.updateSearchParam(this.searchParameters);
                    return this.changePage();
                }
            }
        ]);
        return ItemSearchCtrl;
    }();
    angular.module('ps.search.item', [
        'ps.dataService.donwloadManager',
        'ps.dataService.item',
        'ps.dataService.tag',
        'ps.player',
        'ps.config.route',
        'ngTagsInput',
        'ngStorage'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        $routeProvider.when('/items', {
            templateUrl: 'html/items-search.html',
            controller: 'ItemsSearchCtrl',
            controllerAs: 'isc',
            reloadOnSearch: false,
            hotkeys: [
                [
                    'right',
                    'Next page',
                    'isc.swipePage(1)'
                ],
                [
                    'left',
                    'Previous page',
                    'isc.swipePage(-1)'
                ]
            ].concat(commonKey),
            resolve: {
                items: ["itemService", "SearchItemCache", function items(itemService, SearchItemCache) {
                    return itemService.search(SearchItemCache.getParameters());
                }]
            }
        });
    }]).constant('DefaultItemSearchParameters', {
        page: 0,
        size: 12,
        term: undefined,
        tags: undefined,
        direction: 'DESC',
        properties: 'pubdate'
    }).controller('ItemsSearchCtrl', ItemSearchCtrl).service('SearchItemCache', SearchItemCache);
    angular.module('ps.search', ['ps.search.item']);
    var StatsController = function () {
        function StatsController(statService, stats) {
            _classCallCheck(this, StatsController);
            this.statService = statService;
            this.month = 1;
            this.chartSeries = [];
            this.transform(stats);
            this.chartConfig = {
                options: {
                    chart: { type: 'spline' },
                    plotOptions: { spline: { marker: { enabled: true } } },
                    xAxis: {
                        type: 'datetime',
                        dateTimeLabelFormats: {
                            // don't display the dummy year
                            month: '%e. %b',
                            year: '%b'
                        },
                        title: { text: 'Date' }
                    }
                },
                series: this.chartSeries,
                title: { text: '' },
                credits: { enabled: false },
                loading: false
            };
        }
        StatsController.$inject = ["statService", "stats"];
        _createClass(StatsController, [
            {
                key: 'transform',
                value: function transform(stats) {
                    var _this12 = this;
                    _.updateinplace(this.chartSeries, []);
                    angular.forEach(stats, function (value) {
                        var element = { name: value.type };
                        element.data = _(value.values).map(StatsController.dateMapper()).sortBy('date').map(StatsController.highChartsMapper()).value();
                        _this12.chartSeries.push(element);
                    });
                }
            },
            {
                key: 'navigate',
                value: function navigate(offset) {
                    this.month += offset;
                    this.generateChartData();
                }
            },
            {
                key: 'generateChartData',
                value: function generateChartData() {
                    var _this13 = this;
                    /*_.updateinplace(this.chartSeries, []);*/
                    return this.statService.statsByType(this.month).then(function (statsByType) {
                        return _this13.transform(statsByType);
                    });
                }
            }
        ], [
            {
                key: 'dateMapper',
                value: function dateMapper() {
                    return function (value) {
                        return {
                            date: Date.UTC(value.date[0], value.date[1] - 1, value.date[2]),
                            numberOfItems: value.numberOfItems
                        };
                    };
                }
            },
            {
                key: 'highChartsMapper',
                value: function highChartsMapper() {
                    return function (value) {
                        return [
                            value.date,
                            value.numberOfItems
                        ];
                    };
                }
            }
        ]);
        return StatsController;
    }();
    angular.module('ps.stats', ['ps.dataService.stat']).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        $routeProvider.when('/stats', {
            templateUrl: 'html/stats.html',
            controller: 'StatsController',
            controllerAs: 'sc',
            hotkeys: commonKey,
            resolve: {
                stats: ["statService", function stats(statService) {
                    return statService.statsByType();
                }]
            }
        });
    }]).controller('StatsController', StatsController);
    /**
 * Created by kevin on 02/11/14.
 */
    angular.module('ps.dataservice', [
        'ps.dataService.donwloadManager',
        'ps.dataService.item',
        'ps.dataService.podcast',
        'ps.dataService.tag',
        'ps.dataService.type',
        'ps.dataService.updateService'
    ]);
    var wsDownloadManager = function () {
        /*@ngNoInject*/
        function wsDownloadManager(urlBase, ngstomp) {
            _classCallCheck(this, wsDownloadManager);
            this.WS_DOWNLOAD_BASE = urlBase;
            this.ngstomp = ngstomp;
        }
        _createClass(wsDownloadManager, [
            {
                key: 'connect',
                value: function connect() {
                    return this.ngstomp.connect();
                }
            },
            {
                key: 'subscribe',
                value: function subscribe(url, callback, scope) {
                    return this.ngstomp.subscribe(url, callback, scope);
                }
            },
            {
                key: 'unsubscribe',
                value: function unsubscribe(url) {
                    return this.ngstomp.unsubscribe(url);
                }
            },
            {
                key: 'toggle',
                value: function toggle(item) {
                    return this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/toogle', item);
                }
            },
            {
                key: 'start',
                value: function start(item) {
                    return this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/start', item);
                }
            },
            {
                key: 'pause',
                value: function pause(item) {
                    return this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/pause', item);
                }
            },
            {
                key: 'stop',
                value: function stop(item) {
                    return this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/stop', item);
                }
            }
        ]);
        return wsDownloadManager;
    }();
    var DownloadManager = function () {
        function DownloadManager(Restangular, ngstomp) {
            _classCallCheck(this, DownloadManager);
            this.Restangular = Restangular;
            this.baseTask = this.Restangular.one('task');
            this.baseDownloadManager = this.baseTask.one('downloadManager');
            this.WS_DOWNLOAD_BASE = '/app/download';
            this.ws = new wsDownloadManager(this.WS_DOWNLOAD_BASE, ngstomp);
        }
        DownloadManager.$inject = ["Restangular", "ngstomp"];
        _createClass(DownloadManager, [
            {
                key: 'download',
                value: function download(item) {
                    return this.Restangular.one('item').customGET(item.id + '/addtoqueue');
                }
            },
            {
                key: 'stopDownload',
                value: function stopDownload(item) {
                    return this.baseDownloadManager.customPOST(item.id, 'stopDownload');
                }
            },
            {
                key: 'toggleDownload',
                value: function toggleDownload(item) {
                    return this.baseDownloadManager.customPOST(item.id, 'toogleDownload');
                }
            },
            {
                key: 'stopAllDownload',
                value: function stopAllDownload() {
                    return this.baseDownloadManager.customGET('stopAllDownload');
                }
            },
            {
                key: 'pauseAllDownload',
                value: function pauseAllDownload() {
                    return this.baseDownloadManager.customGET('pauseAllDownload');
                }
            },
            {
                key: 'restartAllCurrentDownload',
                value: function restartAllCurrentDownload() {
                    return this.baseDownloadManager.customGET('restartAllCurrentDownload');
                }
            },
            {
                key: 'removeFromQueue',
                value: function removeFromQueue(item) {
                    return this.baseDownloadManager.customDELETE('queue/' + item.id);
                }
            },
            {
                key: 'updateNumberOfSimDl',
                value: function updateNumberOfSimDl(number) {
                    return this.baseDownloadManager.customPOST(number, 'limit');
                }
            },
            {
                key: 'dontDonwload',
                value: function dontDonwload(item) {
                    return this.baseDownloadManager.customDELETE('queue/' + item.id + '/andstop');
                }
            },
            {
                key: 'getDownloading',
                value: function getDownloading() {
                    return this.baseTask.all('downloadManager/downloading').getList();
                }
            },
            {
                key: 'getNumberOfSimDl',
                value: function getNumberOfSimDl() {
                    return this.baseDownloadManager.one('limit').get();
                }
            },
            {
                key: 'moveInWaitingList',
                value: function moveInWaitingList(item, position) {
                    this.baseDownloadManager.customPOST({
                        id: item.id,
                        position: position
                    }, 'move');
                }
            }
        ]);
        return DownloadManager;
    }();
    angular.module('ps.dataService.donwloadManager', [
        'restangular',
        'AngularStompDK'
    ]).service('DonwloadManager', DownloadManager);
    /**
 * Created by kevin on 01/11/14.
 */
    var itemService = function () {
        function itemService(Restangular) {
            _classCallCheck(this, itemService);
            this.Restangular = Restangular;
        }
        itemService.$inject = ["Restangular"];
        _createClass(itemService, [
            {
                key: 'search',
                value: function search() {
                    var _this14 = this;
                    var searchParameters = arguments[0] === undefined ? {
                        page: 0,
                        size: 12
                    } : arguments[0];
                    return this.Restangular.one('item/search').post(null, searchParameters).then(function (responseFromServer) {
                        responseFromServer.content = _this14.restangularizedItems(responseFromServer.content);
                        return responseFromServer;
                    });
                }
            },
            {
                key: 'findById',
                value: function findById(podcastId, itemId) {
                    return this.Restangular.one('podcast', podcastId).one('items', itemId).get();
                }
            },
            {
                key: 'getItemForPodcastWithPagination',
                value: function getItemForPodcastWithPagination(podcast, pageParemeters) {
                    return podcast.one('items').post(null, pageParemeters);
                }
            },
            {
                key: 'restangularizePodcastItem',
                value: function restangularizePodcastItem(podcast, items) {
                    return this.Restangular.restangularizeCollection(podcast, items, 'items');
                }
            },
            {
                key: 'restangularizedItems',
                value: function restangularizedItems(itemList) {
                    var _this15 = this;
                    var restangularList = [];
                    angular.forEach(itemList, function (value) {
                        restangularList.push(_this15.Restangular.restangularizeElement(_this15.Restangular.one('podcast', value.podcastId), value, 'items'));
                    });
                    return restangularList;
                }
            }
        ]);
        return itemService;
    }();
    angular.module('ps.dataService.item', ['restangular']).service('itemService', itemService);
    /**
 * Created by kevin on 02/11/14.
 */
    var podcastService = function () {
        function podcastService(Restangular) {
            _classCallCheck(this, podcastService);
            this.Restangular = Restangular;
            this.route = 'podcast';
        }
        podcastService.$inject = ["Restangular"];
        _createClass(podcastService, [
            {
                key: 'findById',
                value: function findById(podcastId) {
                    return this.Restangular.one(this.route, podcastId).get();
                }
            },
            {
                key: 'findAll',
                value: function findAll() {
                    return this.Restangular.all(this.route).getList();
                }
            },
            {
                key: 'save',
                value: function save(podcast) {
                    return podcast.save();
                }
            },
            {
                key: 'getNewPodcast',
                value: function getNewPodcast() {
                    return this.Restangular.one(this.route);
                }
            },
            {
                key: 'patch',
                value: function patch(item) {
                    return item.patch();
                }
            },
            {
                key: 'deletePodcast',
                value: function deletePodcast(item) {
                    return item.remove();
                }
            },
            {
                key: 'findInfo',
                value: function findInfo(url) {
                    return this.Restangular.one(this.route).findInfo(url);
                }
            },
            {
                key: 'statsByPubdate',
                value: function statsByPubdate(id) {
                    var numberOfMonth = arguments[1] === undefined ? 6 : arguments[1];
                    return this.Restangular.one(this.route, id).one('stats').all('byPubdate').post(numberOfMonth);
                }
            },
            {
                key: 'statsByByDownloaddate',
                value: function statsByByDownloaddate(id) {
                    var numberOfMonth = arguments[1] === undefined ? 6 : arguments[1];
                    return this.Restangular.one(this.route, id).one('stats').all('byDownloaddate').post(numberOfMonth);
                }
            }
        ]);
        return podcastService;
    }();
    angular.module('ps.dataService.podcast', ['restangular']).config(["RestangularProvider", function (RestangularProvider) {
        RestangularProvider.addElementTransformer('podcast', false, function (podcast) {
            podcast.addRestangularMethod('findInfo', 'post', 'fetch', undefined, { 'Content-Type': 'text/plain' });
            return podcast;
        });
    }]).service('podcastService', podcastService);
    /**
 * Created by kevin on 01/11/14.
 */
    var statService = function () {
        function statService(Restangular) {
            _classCallCheck(this, statService);
            this.Restangular = Restangular;
            this.base = this.Restangular.one('stats');
        }
        statService.$inject = ["Restangular"];
        _createClass(statService, [{
                key: 'statsByType',
                value: function statsByType() {
                    var numberOfMonth = arguments[0] === undefined ? 1 : arguments[0];
                    return this.base.all('byType').post(numberOfMonth);
                }
            }]);
        return statService;
    }();
    angular.module('ps.dataService.stat', ['restangular']).service('statService', statService);
    /**
 * Created by kevin on 01/11/14.
 */
    var tagService = function () {
        function tagService(Restangular) {
            _classCallCheck(this, tagService);
            this.baseAll = Restangular.all('tag');
        }
        tagService.$inject = ["Restangular"];
        _createClass(tagService, [
            {
                key: 'getAll',
                value: function getAll() {
                    return this.baseAll.get();
                }
            },
            {
                key: 'search',
                value: function search(query) {
                    return this.baseAll.post(null, { name: query });
                }
            }
        ]);
        return tagService;
    }();
    angular.module('ps.dataService.tag', ['restangular']).service('tagService', tagService);
    /**
 * Created by kevin on 01/11/14.
 */
    var typeService = function () {
        function typeService(Restangular) {
            _classCallCheck(this, typeService);
            this.baseAll = Restangular.all('types');
        }
        typeService.$inject = ["Restangular"];
        _createClass(typeService, [{
                key: 'findAll',
                value: function findAll() {
                    return this.baseAll.getList();
                }
            }]);
        return typeService;
    }();
    angular.module('ps.dataService.type', ['restangular']).service('typeService', typeService);
    var UpdateService = function () {
        function UpdateService(Restangular) {
            _classCallCheck(this, UpdateService);
            this.Restangular = Restangular;
        }
        UpdateService.$inject = ["Restangular"];
        _createClass(UpdateService, [{
                key: 'forceUpdatePodcast',
                value: function forceUpdatePodcast(idPodcast) {
                    return this.Restangular.one('task').customPOST(idPodcast, 'updateManager/updatePodcast/force');
                }
            }]);
        return UpdateService;
    }();
    angular.module('ps.dataService.updateService', ['restangular']).service('UpdateService', UpdateService);
    var podcastEditionDirective = function podcastEditionDirective() {
        _classCallCheck(this, podcastEditionDirective);
        this.restrict = 'E';
        this.templateUrl = 'html/podcast-details-edition.html';
        this.scope = { podcast: '=' };
        this.controller = 'podcastEditionCtrl';
        this.controllerAs = 'pec';
        this.bindToController = true;
    };
    var podcastEditionCtrl = function () {
        function podcastEditionCtrl($scope, $location, tagService, podcastService) {
            _classCallCheck(this, podcastEditionCtrl);
            this.$scope = $scope;
            this.$location = $location;
            this.tagService = tagService;
            this.podcastService = podcastService;
        }
        podcastEditionCtrl.$inject = ["$scope", "$location", "tagService", "podcastService"];
        _createClass(podcastEditionCtrl, [
            {
                key: 'loadTags',
                value: function loadTags(query) {
                    return this.tagService.search(query);
                }
            },
            {
                key: 'save',
                value: function save() {
                    var _this16 = this;
                    var podcastToUpdate = _.cloneDeep(this.podcast);
                    podcastToUpdate.items = null;
                    return this.podcastService.patch(podcastToUpdate).then(function (patchedPodcast) {
                        return _.assign(_this16.podcast, patchedPodcast);
                    }).then(function () {
                        return _this16.$scope.$emit('podcastEdition:save');
                    });
                }
            },
            {
                key: 'deletePodcast',
                value: function deletePodcast() {
                    var _this17 = this;
                    return this.podcastService.deletePodcast(this.podcast).then(function () {
                        return _this17.$location.path('/podcasts');
                    });
                }
            }
        ]);
        return podcastEditionCtrl;
    }();
    angular.module('ps.podcast.details.edition', [
        'ps.dataService.podcast',
        'ps.dataService.tag',
        'ngTagsInput'
    ]).directive('podcastEdition', function () {
        return new podcastEditionDirective();
    }).controller('podcastEditionCtrl', podcastEditionCtrl);
    var podcastItemsListDirective = function podcastItemsListDirective() {
        _classCallCheck(this, podcastItemsListDirective);
        this.restrict = 'E';
        this.templateUrl = 'html/podcast-details-episodes.html';
        this.scope = { podcast: '=' };
        this.controller = 'podcastItemsListCtrl';
        this.controllerAs = 'pic';
        this.bindToController = true;
    };
    var podcastItemsListCtrl = function () {
        function podcastItemsListCtrl($scope, DonwloadManager, PodcastItemPerPage, itemService, playlistService) {
            var _this18 = this;
            _classCallCheck(this, podcastItemsListCtrl);
            /* DI */
            this.$scope = $scope;
            this.DownloadManager = DonwloadManager;
            this.itemService = itemService;
            this.playlistService = playlistService;
            this.currentPage = 1;
            this.itemPerPage = PodcastItemPerPage;
            this.loadPage();
            this.$scope.$on('podcastItems:refresh', function () {
                _this18.currentPage = 1;
                _this18.loadPage();
            });
            this.DownloadManager.ws.subscribe('/topic/podcast/'.concat(this.podcast.id), function (message) {
                return _this18.onMessageFromWS(message);
            }, $scope);
        }
        podcastItemsListCtrl.$inject = ["$scope", "DonwloadManager", "PodcastItemPerPage", "itemService", "playlistService"];
        _createClass(podcastItemsListCtrl, [
            {
                key: 'onMessageFromWS',
                value: function onMessageFromWS(message) {
                    var item = JSON.parse(message.body);
                    var elemToUpdate = _.find(this.podcast.items, { id: item.id });
                    _.assign(elemToUpdate, item);
                }
            },
            {
                key: 'loadPage',
                value: function loadPage() {
                    var _this19 = this;
                    this.currentPage = this.currentPage < 1 ? 1 : this.currentPage > Math.ceil(this.totalItems / this.itemPerPage) ? Math.ceil(this.totalItems / this.itemPerPage) : this.currentPage;
                    return this.itemService.getItemForPodcastWithPagination(this.podcast, {
                        size: this.itemPerPage,
                        page: this.currentPage - 1,
                        direction: 'DESC',
                        properties: 'pubdate'
                    }).then(function (itemsResponse) {
                        _this19.podcast.items = _this19.itemService.restangularizePodcastItem(_this19.podcast, itemsResponse.content);
                        _this19.podcast.totalItems = itemsResponse.totalElements;
                    });
                }
            },
            {
                key: 'remove',
                value: function remove(item) {
                    var _this20 = this;
                    item.remove().then(function () {
                        return _this20.podcast.items = _.reject(_this20.podcast.items, function (elem) {
                            return elem.id === item.id;
                        });
                    }).then(function () {
                        return _this20.playlistService.remove(item);
                    }).then(function () {
                        return _this20.loadPage();
                    });
                }
            },
            {
                key: 'reset',
                value: function reset(item) {
                    var _this21 = this;
                    return item.reset().then(function (itemReseted) {
                        var itemInList = _.find(_this21.podcast.items, { id: itemReseted.id });
                        _.assign(itemInList, itemReseted);
                        return itemInList;
                    }).then(function (itemToRemove) {
                        return _this21.playlistService.remove(itemToRemove);
                    });
                }
            },
            {
                key: 'addOrRemoveInPlaylist',
                value: function addOrRemoveInPlaylist(item) {
                    this.playlistService.addOrRemove(item);
                }
            },
            {
                key: 'isInPlaylist',
                value: function isInPlaylist(item) {
                    return this.playlistService.contains(item);
                }
            },
            {
                key: 'swipePage',
                value: function swipePage(val) {
                    this.currentPage += val;
                    this.loadPage();
                }
            },
            {
                key: 'stopDownload',
                value: function stopDownload(item) {
                    this.DownloadManager.ws.stop(item);
                }
            },
            {
                key: 'toggleDownload',
                value: function toggleDownload(item) {
                    this.DownloadManager.ws.toggle(item);
                }
            }
        ]);
        return podcastItemsListCtrl;
    }();
    angular.module('ps.podcast.details.episodes', ['ps.player']).directive('podcastItemsList', function () {
        return new podcastItemsListDirective();
    }).constant('PodcastItemPerPage', 10).controller('podcastItemsListCtrl', podcastItemsListCtrl);
    var PodcastDetailCtrl = function () {
        function PodcastDetailCtrl($scope, podcast, UpdateService) {
            var _this22 = this;
            _classCallCheck(this, PodcastDetailCtrl);
            this.$scope = $scope;
            this.UpdateService = UpdateService;
            this.podcast = podcast;
            this.podcastTabs = [
                {
                    heading: 'Episodes',
                    active: true
                },
                {
                    heading: 'Edition',
                    active: false
                },
                {
                    heading: 'Upload',
                    disabled: this.podcast.type !== 'send'
                },
                {
                    heading: 'Stats',
                    active: false
                }
            ];
            this.$scope.$on('podcastEdition:save', function () {
                return _this22.refreshItems();
            });
        }
        PodcastDetailCtrl.$inject = ["$scope", "podcast", "UpdateService"];
        _createClass(PodcastDetailCtrl, [
            {
                key: 'refreshItems',
                value: function refreshItems() {
                    this.$scope.$broadcast('podcastItems:refresh');
                }
            },
            {
                key: 'refresh',
                value: function refresh() {
                    var _this23 = this;
                    this.UpdateService.forceUpdatePodcast(this.podcast.id).then(function () {
                        return _this23.refreshItems();
                    });
                }
            },
            {
                key: 'tabsActive',
                value: function tabsActive(num) {
                    this.podcastTabs[num].active = true;
                }
            }
        ]);
        return PodcastDetailCtrl;
    }();
    angular.module('ps.podcast.details', [
        'ps.config.route',
        'ps.podcast.details',
        'ps.podcast.details.episodes',
        'ps.podcast.details.edition',
        'ps.podcast.details.upload',
        'ps.podcast.details.stats',
        'ps.dataService.updateService'
    ]).config(["$routeProvider", "commonKey", function ($routeProvider, commonKey) {
        return $routeProvider.when('/podcasts/:podcastId', {
            templateUrl: 'html/podcast-detail.html',
            controller: 'PodcastDetailCtrl',
            controllerAs: 'pdc',
            /*hotkeys: [
            ['r', 'Refresh', 'pdc.refreshItems()'],
            ['f', 'Force Refresh', 'pdc.refresh()'],
            ['l', 'List of Items', 'pdc.tabsActive(0)'],
            ['m', 'Modification of Podcast', 'pdc.tabsActive(1)']
        ].concat(commonKey),*/
            hotkeys: commonKey,
            resolve: {
                podcast: ["podcastService", "$route", function podcast(podcastService, $route) {
                    return podcastService.findById($route.current.params.podcastId);
                }]
            }
        });
    }]).controller('PodcastDetailCtrl', PodcastDetailCtrl);
    var podcastStatsDirective = function podcastStatsDirective() {
        _classCallCheck(this, podcastStatsDirective);
        this.restrict = 'E';
        this.scope = { podcast: '=' };
        this.templateUrl = 'html/podcast-details-stats.html';
        this.controller = 'PodcastDetailsStatsCtrl';
        this.controllerAs = 'pdsc';
        this.bindToController = true;
    };
    var PodcastDetailsStatsCtrl = function () {
        function PodcastDetailsStatsCtrl($scope, $q, podcastService) {
            var _this24 = this;
            _classCallCheck(this, PodcastDetailsStatsCtrl);
            this.$q = $q;
            this.podcastService = podcastService;
            this.month = 6;
            this.chartSeries = [];
            this.generateChartData();
            this.chartConfig = {
                options: {
                    chart: { type: 'spline' },
                    plotOptions: { spline: { marker: { enabled: true } } },
                    xAxis: {
                        type: 'datetime',
                        dateTimeLabelFormats: {
                            // don't display the dummy year
                            month: '%e. %b',
                            year: '%b'
                        },
                        title: { text: 'Date' }
                    }
                },
                series: this.chartSeries,
                title: { text: '' },
                credits: { enabled: false },
                loading: false
            };
            $scope.$on('podcastItems:refresh', function () {
                return _this24.generateChartData();
            });
        }
        PodcastDetailsStatsCtrl.$inject = ["$scope", "$q", "podcastService"];
        _createClass(PodcastDetailsStatsCtrl, [
            {
                key: 'navigate',
                value: function navigate(offset) {
                    this.month += offset;
                    return this.generateChartData();
                }
            },
            {
                key: 'generateChartData',
                value: function generateChartData() {
                    var _this25 = this;
                    PodcastDetailsStatsCtrl.resetChart(this.chartSeries);
                    return this.$q.all([
                        this.podcastService.statsByByDownloaddate(this.podcast.id, this.month),
                        this.podcastService.statsByPubdate(this.podcast.id, this.month)
                    ]).then(function (arrayResult) {
                        var downloadData = _(arrayResult[0]).map(PodcastDetailsStatsCtrl.dateMapper()).sortBy('date').map(PodcastDetailsStatsCtrl.highChartsMapper()).value(), publicationData = _(arrayResult[1]).map(PodcastDetailsStatsCtrl.dateMapper()).sortBy('date').map(PodcastDetailsStatsCtrl.highChartsMapper()).value();
                        _this25.chartSeries.push({
                            name: 'Download Date',
                            data: downloadData
                        });
                        _this25.chartSeries.push({
                            name: 'Publication Date',
                            data: publicationData
                        });
                        return _this25.chartSeries;
                    });
                }
            }
        ], [
            {
                key: 'resetChart',
                value: function resetChart(chartSeries) {
                    _.updateinplace(chartSeries, []);
                }
            },
            {
                key: 'dateMapper',
                value: function dateMapper() {
                    return function (value) {
                        return {
                            date: Date.UTC(value.date[0], value.date[1] - 1, value.date[2]),
                            numberOfItems: value.numberOfItems
                        };
                    };
                }
            },
            {
                key: 'highChartsMapper',
                value: function highChartsMapper() {
                    return function (value) {
                        return [
                            value.date,
                            value.numberOfItems
                        ];
                    };
                }
            }
        ]);
        return PodcastDetailsStatsCtrl;
    }();
    angular.module('ps.podcast.details.stats', ['highcharts-ng']).directive('podcastStats', function () {
        return new podcastStatsDirective();
    }).controller('PodcastDetailsStatsCtrl', PodcastDetailsStatsCtrl);
    angular.module('ps.podcast.details.upload', ['angularFileUpload']).directive('podcastUpload', function () {
        return {
            restrcit: 'E',
            templateUrl: 'html/podcast-details-upload.html',
            scope: { podcast: '=' },
            controller: 'podcastUploadCtrl'
        };
    }).controller('podcastUploadCtrl', ["$scope", "$log", function ($scope, $log) {
        $scope.onFileSelect = function ($files) {
            var formData;
            angular.forEach($files, function (file) {
                formData = new FormData();
                formData.append('file', file);
                $scope.podcast.all('items').withHttpConfig({ transformRequest: angular.identity }).customPOST(formData, 'upload', undefined, { 'Content-Type': undefined }).then(function (item) {
                    $log.info('Upload de l\'item suivant');
                    $log.info(item);
                });
            });
        };
    }]);
}());