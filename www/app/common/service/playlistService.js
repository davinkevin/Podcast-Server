import {Module, Service} from '../../decorators';
import _ from 'lodash';
import angular from 'angular';
import NgStorage from 'ngstorage';

@Module({
    name : 'ps.common.service.playlist',
    modules : [ NgStorage ]
})
@Service('playlistService')
export default class PlaylistService {

    constructor($localStorage) {
        "ngInject";
        this.$localStorage = $localStorage;
        this.$localStorage.playlist = this.$localStorage.playlist || [];
    }

    playlist() {
        return this.$localStorage.playlist;
    }
    add(item) {
        this.$localStorage.playlist.push(item);
    }
    remove (item) {
        this.$localStorage.playlist = _.remove(this.$localStorage.playlist, function(elem) { return elem.id !== item.id; });
    }
    contains(item) {
        return angular.isObject(_.find(this.$localStorage.playlist, {id : item.id}));
    }
    addOrRemove (item) {
        (this.contains(item)) ? this.remove(item) : this.add(item);
    }
    removeAll () {
        this.$localStorage.playlist = [];
    }
}