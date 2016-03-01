/**
    * Created by kevin on 20/02/2016 for Podcast Server
    */
import {Component, View, Module} from '../../../../decorators';
import template from './vg-copy.html!text';
import './vg-copy.css!';

@Module({
    name : 'ps.common.component.videogular.vgCopy'
})
@Component({
    selector : 'vg-copy',
    as : 'vgcopy',
    bindToController : {
        url : '='
    }
})
@View({
    template : template
})
export default class VgCopy {}

