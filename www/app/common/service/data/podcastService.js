/**
 * Created by kevin on 02/11@/14 for Podcast Server
 */
import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.data.podcastService'
})
@Service('podcastService')
export default class PodcastService  {

    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    findById(id) {
        return this.$http.get(`/api/podcast/${id}`).then(r => r.data);
    }

    findAll() {
        return this.$http.get(`/api/podcast`).then(r => r.data);
    }

    save(podcast) {
        if (podcast.id) {
            return this.$http.put(`/api/podcast/${podcast.id}`, podcast).then(r => r.data);
        }
        return this.$http.post(`/api/podcast`, podcast).then(r => r.data);
    }

    patch(podcast) {
        return this.$http.put(`/api/podcast/${podcast.id}`, podcast).then(r => r.data);
    }

    delete(podcast) {
        return this.$http.delete(`/api/podcast/${podcast.id}`);
    }

    findInfo(url) {
        let headers = {'Content-Type': 'text/plain'};
        return this.$http.post(`/api/podcast/fetch`, url, { headers }).then(r => r.data);
    }

    statsByPubdate(id, numberOfMonth = 6) {
        return this.$http.post(`/api/podcast/${id}/stats/byPubdate`, numberOfMonth).then(r => r.data);
    }

    statsByByDownloaddate(id, numberOfMonth = 6) {
        return this.$http.post(`/api/podcast/${id}/stats/byDownloaddate`, numberOfMonth).then(r => r.data);
    }
}