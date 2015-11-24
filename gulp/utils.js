/**
 * angularjs-jspm-seed
 * Created by kdavin on 20/11/2015.
 */

Array.prototype.flatMap = function(lambda) {
    return Array.prototype.concat.apply([], this.map(lambda));
};