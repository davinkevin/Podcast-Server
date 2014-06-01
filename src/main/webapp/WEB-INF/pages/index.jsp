<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<!doctype html>
<html ng-app="podcastApp">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/js/lib/bootstrap/dist/css/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="/js/lib/angular-loading-bar/build/loading-bar.min.css" rel="stylesheet" type="text/css">
    <link href="/js/lib/animate.css/animate.min.css" rel="stylesheet" type="text/css">
    <link href="/css/podcastserver.css" rel="stylesheet" type="text/css">
    <%--<link rel="stylesheet/less" type="text/css" href="less/podcastserver.less" />--%>
</head>
<body>
<nav class="navbar navbar-inverse" role="navigation">
    <div class="container-fluid">
        <div class="navbar-header">
            <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#compact-panel">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#/items">Podcast Server</a>
        </div>

        <div class="collapse navbar-collapse navbar-ex1-collapse" id="compact-panel">
            <ul class="nav navbar-nav">
                <li>
                    <a href="#/podcasts">
                            Podcast
                    </a>
                </li>
                <li>
                    <a href="#/podcast/add">
                        Ajouter
                    </a>
                </li>
                <li>
                    <a href="#/download">
                        Téléchargement
                    </a>
                </li>
            </ul>
        </div>
    </div>
</nav>

    <div ng-view autoscroll=""></div>

<script src="<c:url value="/js/lib/jquery/dist/jquery.min.js"/>"></script>
<script src="<c:url value="/js/lib/bootstrap/dist/js/bootstrap.min.js"/>"></script>
<script src="<c:url value="/js/lib/lodash/dist/lodash.min.js"/>"></script>
<script src="<c:url value="/js/lib/sockjs/sockjs.min.js"/>"></script>
<script src="<c:url value="/js/lib/stomp-websocket/lib/stomp.min.js"/>"></script>

<script src="<c:url value="/js/lib/angular/angular.min.js"/>"></script>
<script src="<c:url value="/js/lib/angular-animate/angular-animate.min.js"/>"></script>
<script src="<c:url value="/js/lib/angular-route/angular-route.min.js"/>"></script>
<script src="<c:url value="/js/lib/restangular/dist/restangular.min.js"/>"></script>
<script src="<c:url value="/js/lib/angular-bootstrap/ui-bootstrap.min.js"/>"></script>
<script src="<c:url value="/js/lib/angular-bootstrap/ui-bootstrap-tpls.min.js"/>"></script>
<script src="<c:url value="/js/lib/AngularStomp/angular-stomp.js"/>"></script>
<script src="<c:url value="/js/lib/angular-local-storage/angular-local-storage.min.js" />"></script>
<script src="<c:url value="/js/lib/angular-truncate/dist/angular-truncate.min.js" />"></script>
<script src="<c:url value="/js/lib/angular-loading-bar/build/loading-bar.min.js" />"></script>


<%--
<script src="<c:url value="/js/services.js"/>"></script>
<script src="<c:url value="/js/filters.js"/>"></script>
<script src="<c:url value="/js/controllers.js"/>"></script>
<script src="<c:url value="/js/app.js"/>"></script>
--%>
<script src="<c:url value="/js/all.min.js"/>"></script>
<%--
<script src="<c:url value="/js/all.js"/>"></script>
--%>

</body>
</html>