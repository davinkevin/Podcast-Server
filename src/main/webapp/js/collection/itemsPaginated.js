
var ItemsPaginated = Backbone.Paginator.requestPager.extend({

    model: Item,

    paginator_core: {
        type: 'GET',
        dataType: 'json',
        url: '/api/item/pagination'
    },

    paginator_ui: {
        // Paginator Value
        firstPage: 1,
        currentPage: 1,
        perPage: 9,
        totalPages: 10,


        size: 9,
        number: 0,
        //firstPage: true,
        lastPage: false,
        //currentPage: 0,
        //numberPages: 10,
        pagesInRange: 2,
        totalElements: 0,
        direction: "DESC",
        properties:"pubdate"


    },

    server_api:    {
        'size':  function(){
            return this.perPage;
        },

        'page':  function(){
            return this.currentPage-1;
        },

        'direction': function() {
            return this.direction;
        },

        'properties': function() {
            return this.properties;
        }
    },

    parse:  function(response) {
        this.totalPages = parseInt(response.totalPages);
        this.numberOfElements = response.totalElements;
        this.totalRecords = parseInt(response.totalRecords);
        //this.firstPage = response.firstPage;
        //this.lastPage = response.lastPage;
        //this.size = response.size;
        this.perPage = response.size;
        this.number = response.number;
        this.currentPage=response.number+1;
        return response.content;
    }
});