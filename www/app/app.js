import {Boot, Module} from './decorators';
import { TitleComponent } from './common/component/title/title';
import SearchModule from './search/search';
import PodcastsModule from './podcasts/podcasts';
import ItemModule from './item/item';
import DownloadModule from './download/download';
import PlayerModule from './player/player';
import StatsModule from './stats/stats';
import ConfigModule from './config/config';

@Boot({ element : document })
@Module({
    name : 'podcastApp', 
    modules : [ TitleComponent, SearchModule, PodcastsModule, ItemModule, DownloadModule, PlayerModule, StatsModule, ConfigModule ]
})
export default class App {}

