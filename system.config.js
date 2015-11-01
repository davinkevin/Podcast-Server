System.config({
  baseURL: "./",
  defaultJSExtensions: true,
  transpiler: "babel",
  babelOptions: {
    "optional": [
      "runtime",
      "optimisation.modules.system"
    ]
  },
  paths: {
    "github:*": "jspm_packages/github/*",
    "npm:*": "jspm_packages/npm/*"
  },
  buildCSS: true,
  separateCSS: true,

  map: {
    "AngularStompDK": "github:davinkevin/AngularStompDK@0.3.2",
    "angular": "github:angular/bower-angular@1.4.7",
    "angular-animate": "github:angular/bower-angular-animate@1.4.7",
    "angular-bootstrap": "github:angular-ui/bootstrap-bower@0.14.3",
    "angular-hotkeys": "github:chieffancypants/angular-hotkeys@1.4.5",
    "angular-loading-bar": "github:chieffancypants/angular-loading-bar@0.8.0",
    "angular-mocks": "npm:angular-mocks@1.4.7",
    "angular-notification": "github:neoziro/angular-notification@1.1.1",
    "angular-route": "github:angular/bower-angular-route@1.4.7",
    "angular-sanitize": "github:angular/bower-angular-sanitize@1.4.7",
    "angular-touch": "github:angular/bower-angular-touch@1.4.7",
    "angular-truncate": "github:sparkalow/angular-truncate@master",
    "animate.css": "github:daneden/animate.css@3.4.0",
    "babel": "npm:babel-core@5.8.25",
    "babel-runtime": "npm:babel-runtime@5.8.25",
    "bootstrap-less": "github:distros/bootstrap-less@3.3.9",
    "clean-css": "npm:clean-css@3.4.6",
    "core-js": "npm:core-js@1.2.2",
    "css": "github:systemjs/plugin-css@0.1.19",
    "font-awesome": "github:components/font-awesome@4.4.0",
    "highcharts-ng": "github:pablojim/highcharts-ng@0.0.11",
    "highcharts-release": "github:highslide-software/highcharts-release@4.1.9",
    "ionicons": "github:driftyco/ionicons@2.0.1",
    "lodash": "github:lodash/lodash@3.10.1",
    "ng-file-upload": "github:danialfarid/ng-file-upload@9.1.1",
    "ng-tags-input": "github:mbenford/ngTagsInput@3.0.0",
    "ngstorage": "github:gsklee/ngStorage@0.3.9",
    "pablojim/highcharts-ng": "github:pablojim/highcharts-ng@0.0.11",
    "restangular": "github:mgonto/restangular@1.5.1",
    "sockjs-client": "github:sockjs/sockjs-client@0.3.4",
    "stomp-websocket": "github:jmesnil/stomp-websocket@2.3.4",
    "systemjs/plugin-text": "github:systemjs/plugin-text@0.0.2",
    "text": "github:systemjs/plugin-text@0.0.2",
    "videogular": "github:2fdevs/bower-videogular@1.3.2",
    "videogular-buffering": "github:2fdevs/bower-videogular-buffering@1.3.2",
    "videogular-controls": "github:2fdevs/bower-videogular-controls@1.3.2",
    "videogular-overlay-play": "github:2fdevs/bower-videogular-overlay-play@1.3.2",
    "videogular-poster": "github:2fdevs/bower-videogular-poster@1.3.2",
    "videogular-themes-default": "github:2fdevs/bower-videogular-themes-default@1.3.2",
    "github:angular/bower-angular-animate@1.4.7": {
      "angular": "github:angular/bower-angular@1.4.7"
    },
    "github:angular/bower-angular-route@1.4.7": {
      "angular": "github:angular/bower-angular@1.4.7"
    },
    "github:angular/bower-angular-sanitize@1.4.7": {
      "angular": "github:angular/bower-angular@1.4.7"
    },
    "github:angular/bower-angular-touch@1.4.7": {
      "angular": "github:angular/bower-angular@1.4.7"
    },
    "github:chieffancypants/angular-hotkeys@1.4.5": {
      "angular": "github:angular/bower-angular@1.4.7"
    },
    "github:chieffancypants/angular-loading-bar@0.8.0": {
      "angular": "github:angular/bower-angular@1.4.7",
      "css": "github:systemjs/plugin-css@0.1.19"
    },
    "github:davinkevin/AngularStompDK@0.3.2": {
      "angular": "github:angular/bower-angular@1.4.7",
      "stomp-websocket": "github:jmesnil/stomp-websocket@2.3.4"
    },
    "github:distros/bootstrap-less@3.3.9": {
      "jquery": "github:components/jquery@2.1.4"
    },
    "github:jspm/nodelibs-assert@0.1.0": {
      "assert": "npm:assert@1.3.0"
    },
    "github:jspm/nodelibs-buffer@0.1.0": {
      "buffer": "npm:buffer@3.5.1"
    },
    "github:jspm/nodelibs-events@0.1.1": {
      "events": "npm:events@1.0.2"
    },
    "github:jspm/nodelibs-http@1.7.1": {
      "Base64": "npm:Base64@0.2.1",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "stream": "github:jspm/nodelibs-stream@0.1.0",
      "url": "github:jspm/nodelibs-url@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "github:jspm/nodelibs-https@0.1.0": {
      "https-browserify": "npm:https-browserify@0.0.0"
    },
    "github:jspm/nodelibs-os@0.1.0": {
      "os-browserify": "npm:os-browserify@0.1.2"
    },
    "github:jspm/nodelibs-path@0.1.0": {
      "path-browserify": "npm:path-browserify@0.0.0"
    },
    "github:jspm/nodelibs-process@0.1.2": {
      "process": "npm:process@0.11.2"
    },
    "github:jspm/nodelibs-stream@0.1.0": {
      "stream-browserify": "npm:stream-browserify@1.0.0"
    },
    "github:jspm/nodelibs-url@0.1.0": {
      "url": "npm:url@0.10.3"
    },
    "github:jspm/nodelibs-util@0.1.0": {
      "util": "npm:util@0.10.3"
    },
    "github:mbenford/ngTagsInput@3.0.0": {
      "angular": "github:angular/bower-angular@1.4.7",
      "css": "github:systemjs/plugin-css@0.1.19"
    },
    "github:mgonto/restangular@1.5.1": {
      "angular": "github:angular/bower-angular@1.4.7",
      "lodash": "npm:lodash@3.10.1"
    },
    "npm:amdefine@1.0.0": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "module": "github:jspm/nodelibs-module@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:assert@1.3.0": {
      "util": "npm:util@0.10.3"
    },
    "npm:babel-runtime@5.8.25": {
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:buffer@3.5.1": {
      "base64-js": "npm:base64-js@0.0.8",
      "ieee754": "npm:ieee754@1.1.6",
      "is-array": "npm:is-array@1.0.1"
    },
    "npm:clean-css@3.4.6": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "commander": "npm:commander@2.8.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "http": "github:jspm/nodelibs-http@1.7.1",
      "https": "github:jspm/nodelibs-https@0.1.0",
      "os": "github:jspm/nodelibs-os@0.1.0",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.2",
      "source-map": "npm:source-map@0.4.4",
      "url": "github:jspm/nodelibs-url@0.1.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:commander@2.8.1": {
      "child_process": "github:jspm/nodelibs-child_process@0.1.0",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "graceful-readlink": "npm:graceful-readlink@1.0.1",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:core-js@1.2.2": {
      "fs": "github:jspm/nodelibs-fs@0.1.2",
      "path": "github:jspm/nodelibs-path@0.1.0",
      "process": "github:jspm/nodelibs-process@0.1.2",
      "systemjs-json": "github:systemjs/plugin-json@0.1.0"
    },
    "npm:core-util-is@1.0.1": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0"
    },
    "npm:graceful-readlink@1.0.1": {
      "fs": "github:jspm/nodelibs-fs@0.1.2"
    },
    "npm:https-browserify@0.0.0": {
      "http": "github:jspm/nodelibs-http@1.7.1"
    },
    "npm:inherits@2.0.1": {
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:lodash@3.10.1": {
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:os-browserify@0.1.2": {
      "os": "github:jspm/nodelibs-os@0.1.0"
    },
    "npm:path-browserify@0.0.0": {
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:process@0.11.2": {
      "assert": "github:jspm/nodelibs-assert@0.1.0"
    },
    "npm:punycode@1.3.2": {
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:readable-stream@1.1.13": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0",
      "core-util-is": "npm:core-util-is@1.0.1",
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "isarray": "npm:isarray@0.0.1",
      "process": "github:jspm/nodelibs-process@0.1.2",
      "stream-browserify": "npm:stream-browserify@1.0.0",
      "string_decoder": "npm:string_decoder@0.10.31"
    },
    "npm:source-map@0.4.4": {
      "amdefine": "npm:amdefine@1.0.0",
      "process": "github:jspm/nodelibs-process@0.1.2"
    },
    "npm:stream-browserify@1.0.0": {
      "events": "github:jspm/nodelibs-events@0.1.1",
      "inherits": "npm:inherits@2.0.1",
      "readable-stream": "npm:readable-stream@1.1.13"
    },
    "npm:string_decoder@0.10.31": {
      "buffer": "github:jspm/nodelibs-buffer@0.1.0"
    },
    "npm:url@0.10.3": {
      "assert": "github:jspm/nodelibs-assert@0.1.0",
      "punycode": "npm:punycode@1.3.2",
      "querystring": "npm:querystring@0.2.0",
      "util": "github:jspm/nodelibs-util@0.1.0"
    },
    "npm:util@0.10.3": {
      "inherits": "npm:inherits@2.0.1",
      "process": "github:jspm/nodelibs-process@0.1.2"
    }
  }
});
