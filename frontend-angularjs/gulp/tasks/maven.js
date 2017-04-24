/**
 * Created by kevin on 15/04/2016.
 */
import fs from 'fs';
import jsxml from 'node-jsxml';
import paths from '../paths';

jsxml.XML.setSettings({ignoreComments : false, ignoreProcessingInstructions : false, createMainDocument: true});

export default function update(version) {
    var xmlDoc = new jsxml.XML(fs.readFileSync(paths.pomXml, 'utf8'));
    var node = xmlDoc.child('project').child('version');
    if (node != "" && node.getValue() != version) {
        node.setValue(version);
        // Replace is for removing a bug in toXmlString when used from gulp... don't know why !?!?
        fs.writeFileSync(paths.pomXml, xmlDoc.toXMLString().replace(/ ="undefined"/g, ""));
    }
}