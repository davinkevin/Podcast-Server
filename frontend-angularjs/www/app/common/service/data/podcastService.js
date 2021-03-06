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
        return this.$http.get(`/api/v1/podcasts/${id}`).then(r => r.data);
    }

    findAll() {
        return this.$http.get(`/api/v1/podcasts`).then(r => r.data.content);
    }

    save(podcast) {
        if (podcast.id) {
            return this.$http.put(`/api/podcasts/${podcast.id}`, podcast).then(r => r.data);
        }
        return this.$http.post(`/api/v1/podcasts`, podcast).then(r => r.data);
    }

    patch(podcast) {
      return this.$http.put (`/api/v1/podcasts/${podcast.id}`, podcast).then (r => r.data);
    }

    delete(podcast) {
        return this.$http.delete(`/api/v1/podcasts/${podcast.id}`);
    }

    findInfo(url) {
        let headers = {'Content-Type': 'text/plain'};
        return this.$http.post(`/api/v1/podcasts/find`, url, { headers }).then(r => r.data);
    }

    statsByPubDate(id, numberOfMonth = 6) {
        return this.$http.get(`/api/v1/podcasts/${id}/stats/byPubDate?numberOfMonths=${numberOfMonth}`).then(r => r.data);
    }

    statsByByDownloadDate(id, numberOfMonth = 6) {
        return this.$http.get(`/api/v1/podcasts/${id}/stats/byDownloadDate?numberOfMonths=${numberOfMonth}`).then(r => r.data);
    }

    statsByCreationDate(id, numberOfMonth = 6) {
        return this.$http.get(`/api/v1/podcasts/${id}/stats/byCreationDate?numberOfMonths=${numberOfMonth}`).then(r => r.data);
    }

    refresh(id) {
        return this.$http.get(`/api/v1/podcasts/${id}/update`);
    }

    forceRefresh(id) {
        return this.$http.get(`/api/v1/podcasts/${id}/update`);
    }
}
