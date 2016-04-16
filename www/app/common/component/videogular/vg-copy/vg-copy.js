/**
    * Created by kevin on 20/02/2016 for Podcast Server
    */
import {Component, Module} from '../../../../decorators';
import './vg-copy.css!';

@Module({
    name : 'ps.common.component.videogular.vgCopy'
})
@Component({
    selector : 'vg-copy',
    as : 'vgcopy',
    bindings : { url: '='},
    template : `<div><a copy="{{ vgcopy.url }}" class="fa fa-files-o"></a></div>`
})
export default class VgCopy {}

