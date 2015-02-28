<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<!doctype html>
<html ng-app="podcastApp">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta charset="UTF-8">

    <!-- inject:css -->
    <link rel="stylesheet" href="app/js/lib/angular-loading-bar/build/loading-bar.min.css">
    <link rel="stylesheet" href="app/js/lib/animate.css/animate.min.css">
    <link rel="stylesheet" href="app/js/lib/ng-tags-input/ng-tags-input.min.css">
    <link rel="stylesheet" href="app/js/lib/angular-hotkeys/build/hotkeys.min.css">
    <link rel="stylesheet" href="app/js/lib/ionicons/css/ionicons.css">
    <link rel="stylesheet" href="app/js/lib/videogular-themes-default/videogular.min.css">
    <link rel="stylesheet" href="app/js/lib/components-font-awesome/css/font-awesome.min.css">
    <link rel="stylesheet" href="app/js/lib/bootstrap/dist/css/bootstrap.min.css">
    <!-- endinject -->

    <link href="app/css/podcastserver.css" rel="stylesheet" type="text/css">

</head>
<body>
<navbar class="hidden">
    <li>
        <a href="#/podcasts">
            <span class="ionicons ion-social-rss">  </span>
            <span class="hidden-xs">Podcast</span>
        </a>
    </li>
    <li>
        <a href="#/player">
            <span class="ionicons ion-social-youtube"></span>
            <span class="hidden-xs"> Player</span>
        </a>
    </li>
    <li>
        <a href="#/podcast-creation">
            <span class="ionicons ion-android-add"></span>
            <span class="hidden-xs"> Ajouter</span>
        </a>
    </li>
    <li>
        <a href="#/download">
            <span class="ionicons ion-android-download"></span>
            <span class="hidden-xs"> Téléchargement</span>
        </a>
    </li>
</navbar>

<authorize-notification></authorize-notification>

<div ng-view autoscroll=""></div>

<!-- inject:js -->
<script src="app/js/lib/angular/angular.min.js"></script>
<script src="app/js/lib/angular-route/angular-route.min.js"></script>
<script src="app/js/lib/angular-animate/angular-animate.min.js"></script>
<script src="app/js/lib/angular-sanitize/angular-sanitize.min.js"></script>
<script src="app/js/lib/angular-touch/angular-touch.min.js"></script>
<script src="app/js/lib/angular-bootstrap/ui-bootstrap-tpls.min.js"></script>
<script src="app/js/lib/lodash/dist/lodash.compat.min.js"></script>
<script src="app/js/lib/stomp-websocket/lib/stomp.min.js"></script>
<script src="app/js/lib/sockjs/sockjs.min.js"></script>
<script src="app/js/lib/ngstorage/ngStorage.min.js"></script>
<script src="app/js/lib/angular-truncate/src/truncate.js"></script>
<script src="app/js/lib/angular-loading-bar/build/loading-bar.min.js"></script>
<script src="app/js/lib/ng-tags-input/ng-tags-input.min.js"></script>
<script src="app/js/lib/angular-hotkeys/build/hotkeys.min.js"></script>
<script src="app/js/lib/angular-notification/angular-notification.min.js"></script>
<script src="app/js/lib/ng-file-upload/angular-file-upload.min.js"></script>
<script src="app/js/lib/videogular/videogular.js"></script>
<script src="app/js/lib/videogular-poster/poster.js"></script>
<script src="app/js/lib/videogular-buffering/buffering.js"></script>
<script src="app/js/lib/videogular-overlay-play/overlay-play.js"></script>
<script src="app/js/lib/videogular-controls/controls.js"></script>
<script src="app/js/lib/restangular/dist/restangular.min.js"></script>
<script src="app/js/lib/AngularStompDK/dist/angular-stomp.min.js"></script>
<!-- endinject -->

<%--
<script src="app/js/all.min.js"></script>
--%>
<script src="app/js/all.js"></script>

</body>
</html>