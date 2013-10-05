<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page import="lan.dk.podcastserver.entity.*" %>
<%@ page import="java.util.ArrayList"%>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"></script>
    <script src="/js/podcastserver.js" type="text/javascript"></script>
</head>
<body>

<button id="changeToItunes" style="display:inline-block;"> Convertir en flux iTunes</button>
<button id="changeToExternal" style="display:inline-block;"> Convertir en flux externe</button>
<br/>
    <c:forEach items="${listOfPodcast}" var="podcast" >
        <a href="podcast/${podcast.relativeURL}">
            <img width="${podcast.podcastCovertArt.width}" height="${podcast.podcastCovertArt.height}" src="/podcast/${podcast.podcastCovertArt.relativeURLtoCoverArt}">
        </a>
    </c:forEach>
</body>
</html>