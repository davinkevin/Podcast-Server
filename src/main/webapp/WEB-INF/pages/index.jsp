
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <jsp:include page="include/head.jsp" >
        <jsp:param name="title" value="Podcast Server" />
    </jsp:include>
    <body>

    <jsp:include page="include/header.jsp" />

    <div id="listItem" class="container">
        <div class="row">
            <ul class="thumbnails"></ul>
        </div>
        <div clas="row">
            <div class="pagination pagination-centered">

            </div>
        </div>
    </div>

    <script type="text/template" id="item-pagination-template">
        <ul>
            <@ if ( (currentPage < 4 && lastPage < 7) || (currentPage > 4 && lastPage < 7) ) { var borneInf=firstPage; var borneSup=lastPage; }  @>
            <@ if (currentPage < 4 && lastPage > 7) { var borneInf=firstPage; var borneSup=7; }  @>
            <@ if (currentPage >= 4 && lastPage > 7) { var borneInf=currentPage-3; var borneSup=currentPage+3; }  @>
            <@ if (currentPage > lastPage-3 && lastPage > 7) { var borneInf=lastPage-7; var borneSup=lastPage; }  @>

            <li><a class="first" href="#">«</a></li>
            <@ if (currentPage == firstPage) { @>
                <li class="disabled"><a class="prev" href="#">Prev</a></li>
            <@ } else { @>
                <li><a class="prev" href="#">Prev</a></li>
            <@ } @>

            <@ for( p=borneInf;    p < currentPage;  p++) { @>
            <li ><a class="page" href="#"><@= p @></a></li>
            <@ } @>
            <li class="active"><a href="#" ><@= p @></a></li>
            <@ for( p=currentPage+1;    p < borneSup+1;  p++) { @>
            <li><a class="page" href="#"><@= p @></a></li>
            <@ } @>


            <@ if (currentPage == lastPage) { @>
                <li class="disabled"><a class="next" href="#">Next</a></li>
            <@ } else { @>
                <li><a class="next" href="#">Next</a></li>
            <@ } @>
            <li><a class="last" href="#">»</a></li>

        </ul>
    </script>
    <!-- Template Backbone -->
    <script type="text/template" id="item-template">
        <div class="thumbnail right-caption">
            <span class="logo">
                <img src="<@= cover.url @>" width="<@= cover.width @>" height="<@= cover.height @>" alt="">
            </span>
            <div class="caption">
                <h6><@= title @></h6>
                <span class="btn-caption ">
                    <@ if (localUrl == null) { @>
                    <a class="btn btn-download">
                        <i class="icon-download-alt"></i>
                    </a>
                    <a class="btn" href="<@= url @>">
                        <i class="icon-globe"></i>
                    </a>
                    <@ } else { @>
                    <a class="btn btn-play" href="<@= localUrl @>">
                        <i class="icon-play"></i>
                    </a>
                    <@ } @>
                </span>
            </div>
        </div>
    </script>

  <jsp:include page="include/libJavascript.jsp" />

    <!-- Backbone Element -->
    <script src="../../js/model/item.js"></script>
    <script src="../../js/collection/items.js"></script>
    <script src="../../js/collection/itemsPaginated.js"></script>
    <script src="../../js/view/itemView.js"></script>
    <script src="../../js/view/itemsView.js"></script>
    <script>
        //var items = new Items();
        //items.fetch();


        var itemsPaginated = new ItemsPaginated();
        itemsPaginated.fetch();
        var itemsView = new ItemsView({ collection: itemsPaginated });



    </script>

    </body>
</html>