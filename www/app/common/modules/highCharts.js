import {Module} from '../../decorators';
import 'highcharts-dist';
import HighChartsNg from 'highcharts-ng';

@Module({
    name : 'ps.config.highCharts',
    modules : [ HighChartsNg ]
})
export default class HighCharts{}