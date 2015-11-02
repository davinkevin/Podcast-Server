/**
    * Created by kevin on 24/10/2015 for PodcastServer
    */
import angular from 'angular';

export default class SearchItemCache {
    constructor(DefaultItemSearchParameters, $sessionStorage) {
        "ngInject";
        this.$sessionStorage = $sessionStorage;
        this.$sessionStorage.searchParameters = DefaultItemSearchParameters;
    }

    getParameters() {
        return this.$sessionStorage.searchParameters;
    }

    page(pageNumber) {
        if (angular.isNumber(pageNumber)) {
            this.$sessionStorage.searchParameters.page = pageNumber;
        }

        return this.$sessionStorage.searchParameters.page;
    }

    size(sizeNumber) {
        if (angular.isNumber(sizeNumber)) {
            this.$sessionStorage.searchParameters.size = sizeNumber;
        }

        return this.$sessionStorage.searchParameters.size;
    }

    updateSearchParam(searchParam) {
        this.$sessionStorage.searchParameters.term = searchParam.term;
        this.$sessionStorage.searchParameters.tags = searchParam.tags;
        this.$sessionStorage.searchParameters.direction =  searchParam.direction;
        this.$sessionStorage.searchParameters.properties =  searchParam.properties;
        this.$sessionStorage.searchParameters.downloaded = searchParam.downloaded;
    }
}