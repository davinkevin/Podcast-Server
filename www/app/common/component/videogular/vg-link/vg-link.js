/**
    * Created by kevin on 20/02/2016 for Podcast Server
    */
import {Component, View, Module} from '../../../../decorators';
import template from './vg-link.html!text';
import './vg-link.css!';

@Module({
    name : 'ps.common.component.videogular.vgLink'
})
@Component({
    selector : 'vg-link',
    as : 'vglink',
    bindToController : {
        url : '='
    }
})
@View({
    template : template
})
export default class VgLink {}

