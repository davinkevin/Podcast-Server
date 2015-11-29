/**
    * Created by kevin on 14/08/2014 for Podcast Server
    */

import _ from 'lodash';

_.mixin({
    // Update in place, does not preserve order
    updateinplace : function(localArray, remoteArray, comparisonFunction = (inArray, elem) => inArray.indexOf(elem), withOrder = false) {
        /*eslint-disable */
        // Remove from localArray what is not in the remote array :
        _.forEachRight(localArray.slice(), (elem, key) => {
            (comparisonFunction(remoteArray, elem) === -1) && localArray.splice(key, 1);
        });

        // Add to localArray what is new in the remote array :
        _.forEach(remoteArray, (elem) => {
            (comparisonFunction(localArray, elem) === -1) && localArray.push(elem);
        });
        /*eslint-enable */
        if (withOrder) {
            _.forEach(remoteArray, (elem, key) => {
                var elementToMove = localArray.splice(comparisonFunction(localArray, elem), 1)[0];
                localArray.splice(key, 0, elementToMove);
            });
        }
        
        return localArray;
    }
});