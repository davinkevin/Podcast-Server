/**
 * Created by kevin on 14/08/2014.
 */

_.mixin({
    // Update in place, does not preserve order
    updateinplace : function(localArray, remoteArray, comparisonFunction) {
        // Default function working on the === operator by the indexOf function:
        var comparFunc = comparisonFunction || function (inArray, elem) {
            return inArray.indexOf(elem);
        };

        // Remove from localArray what is not in the remote array :
        _.forEachRight(localArray.slice(), function (elem, key) {
            if (comparFunc(remoteArray, elem) === -1) {
                localArray.splice(key, 1);
            }
        });

        // Add to localArray what is new in the remote array :
        _.forEach(remoteArray, function (elem) {
            if (comparFunc(localArray, elem) === -1) {
                localArray.push(elem);
            }
        });

        return localArray;
    }
});