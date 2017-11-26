import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/Observable';
import {Podcast} from '../../entity';

@Injectable()
export class PodcastService {

  constructor(private http: HttpClient) {}

  findAll(): Observable<Podcast[]> {
    return this.http.get<Podcast[]>('/api/podcasts')
  }

} /* istanbul ignore next */
