<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<!doctype html>
<html ng-app="podcastApp">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/js/lib/bootstrap/dist/css/bootstrap.css" rel="stylesheet" type="text/css">
    <link rel="stylesheet/less" type="text/css" href="less/podcastserver.less" />
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

    <div ng-view></div>


<script type="text/javascript">
    less = {
        env: "development", // or "production"
        async: false,       // load imports async
        fileAsync: false,   // load imports async when in a page under
        // a file protocol
        poll: 1000,         // when in watch mode, time in ms between polls
        functions: {},      // user functions, keyed by name
        dumpLineNumbers: "comments", // or "mediaQuery" or "all"
        relativeUrls: false,// whether to adjust url's to be relative
        // if false, url's are already relative to the
        // entry less file
        //rootpath: ":/a.com/"// a path to add on to the start of every url
        //resource
    };
</script>
<script src="<c:url value="/js/lib/less/dist/less-1.6.2.js"/>"></script>
<script src="<c:url value="/js/lib/holderjs/holder.js"/>"></script>
<script src="<c:url value="/js/lib/momentjs/moment.js"/>"></script>
<script src="<c:url value="/js/lib/lodash/dist/lodash.js"/>"></script>
<script src="<c:url value="/js/lib/sockjs/sockjs.js"/>"></script>
<script src="<c:url value="/js/lib/stomp-websocket/lib/stomp.js"/>"></script>
<script src="<c:url value="/js/lib/jquery/jquery.js"/>"></script>
<script src="<c:url value="/js/lib/bootstrap/dist/js/bootstrap.js"/>"></script>

<script src="<c:url value="/js/lib/angular/angular.js"/>"></script>
<script src="<c:url value="/js/lib/angular-route/angular-route.js"/>"></script>
<script src="<c:url value="/js/lib/angular-resource/angular-resource.js"/>"></script>
<script src="<c:url value="/js/lib/restangular/src/restangular.js"/>"></script>
<script src="<c:url value="/js/lib/angular-bootstrap/ui-bootstrap.js"/>"></script>
<script src="<c:url value="/js/lib/angular-bootstrap/ui-bootstrap-tpls.js"/>"></script>
<script src="<c:url value="/js/lib/AngularStomp/angular-stomp.js"/>"></script>
<script src="<c:url value="/js/lib/angular-local-storage/angular-local-storage.js" />"></script>


<script src="<c:url value="/js/services.js"/>"></script>
<script src="<c:url value="/js/filters.js"/>"></script>
<script src="<c:url value="/js/controllers.js"/>"></script>
<script src="<c:url value="/js/app.js"/>"></script>

</body>
</html>