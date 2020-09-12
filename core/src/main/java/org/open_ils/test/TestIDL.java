package org.open_ils.test;
import org.open_ils.idl.*;
import org.opensrf.*;
import org.opensrf.util.*;

public class TestIDL {
    public static void main(String[] args) throws Exception {
        String idlFile = "fm_IDL.xml";
        IDLParser parser = new IDLParser(idlFile);
        parser.parse();

        OSRFObject bre = new OSRFObject("bre");
        bre.put("id", new Integer(1));
        bre.put("isnew", new Boolean(false));
        bre.put("isdeleted", new Boolean(true));
        
        
        System.out.println(bre);
        System.out.println(new JSONWriter(bre).write());
        
        
    }
}
