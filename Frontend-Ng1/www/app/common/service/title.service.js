/**
 * Created by kevin on 07/05/2016.
 */
import {Module, Service} from '../../decorators';
import Rx from 'rx';

@Module({ name : 'ps.common.service.title' })
@Service('TitleService')
export class TitleService {

    title$ = new Rx.BehaviorSubject('Podcast-Server');
    
    set title(title) { this.title$.onNext(title); }
    get title() { return this.title$; }
    
}