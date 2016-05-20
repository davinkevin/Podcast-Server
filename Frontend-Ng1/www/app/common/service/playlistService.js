import {Module, Service} from '../../decorators';
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
    isEmpty() {
        return this.playlist().length === 0;
    }
    play(item) {
        this.removeAll();
        this.add(item);
    }
    add(item) {
        this.playlist().push(item);
    }
    remove (item) {
        this.$localStorage.playlist = this.playlist().filter(elem => elem.id !== item.id);
    }
    contains(item) {
        return !!this.playlist().find(elem => elem.id === item.id);
    }
    addOrRemove(item) {
        this.contains(item) ? this.remove(item) : this.add(item);
    }
    removeAll () {
        this.$localStorage.playlist = [];
    }
}