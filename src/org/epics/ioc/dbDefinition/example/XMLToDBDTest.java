/**
 * 
 */
package org.epics.ioc.dbDefinition.example;

import junit.framework.TestCase;
import org.epics.ioc.dbDefinition.*;
import java.util.*;
public class XMLToDBDTest extends TestCase {
        
    public static void testXML() {
        DBD dbd = DBDFactory.create("test");
        try {
            XMLToDBDFactory.convert(dbd,"/home/mrk/workspace/javaIOC"
                 + "/src/org/epics/ioc/dbDefinition/example/test.xml");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        System.out.printf("\nmenus");
        Map<String,DBDMenu> menuMap = dbd.getMenuMap();
        Set<String> keys = menuMap.keySet();
        for(String key: keys) {
            DBDMenu dbdMenu = menuMap.get(key);
            System.out.print(dbdMenu.toString());
        }
        System.out.printf("\n\nstructures");
        Map<String,DBDStructure> structureMap = dbd.getStructureMap();
        keys = structureMap.keySet();
        for(String key: keys) {
            DBDStructure dbdStructure = structureMap.get(key);
            System.out.print(dbdStructure.toString());
        }
        System.out.printf("\n\nlinkSupport");
        Map<String,DBDLinkSupport> linkSupportMap = dbd.getLinkSupportMap();
        keys = linkSupportMap.keySet();
        for(String key: keys) {
            DBDLinkSupport dbdLinkSupport = linkSupportMap.get(key);
            System.out.print(dbdLinkSupport.toString());
        }
        System.out.printf("\n\nrecordTypes");
        Map<String,DBDRecordType> recordTypeMap = dbd.getRecordTypeMap();
        keys = recordTypeMap.keySet();
        for(String key: keys) {
            DBDRecordType dbdRecordType = recordTypeMap.get(key);
            System.out.print(dbdRecordType.toString());
        }
    }

}
