/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.forth;

import eu.delving.x3ml.X3MLGeneratorPolicy;
import java.util.Date;

/**
 *
 * @author konsolak
 */
public class BMdates implements X3MLGeneratorPolicy.CustomGenerator {
     private String text;
    private Bounds bounds;

    enum Bounds {
        Upper, Lower
    }

    @Override
    public void setArg(String name, String value) throws X3MLGeneratorPolicy.CustomGeneratorException {
        if ("text".equals(name)) {
            text = value;
        } else if ("bound".equals(name)) {
            bounds = Bounds.valueOf(value);
        } else {
            throw new X3MLGeneratorPolicy.CustomGeneratorException("Unrecognized argument name: " + name);
        }
    }

    @Override
    public String getValue() throws X3MLGeneratorPolicy.CustomGeneratorException {
        if (text == null) {
            throw new X3MLGeneratorPolicy.CustomGeneratorException("Missing text argument");
        }
        if (bounds == null) {
            throw new X3MLGeneratorPolicy.CustomGeneratorException("Missing bounds argument");
        }
        return getFormatedDate(bounds.toString(), text);
    }

    @Override
    public String getValueType() throws X3MLGeneratorPolicy.CustomGeneratorException {
        return text.startsWith("http") ? "URI" : "Literal";
    }

    private static String getFormatedDate(String bounds, String time_str) {
        String xsdDate = "";

        try {
            System.out.println("Input date: " + time_str);
            Date formatDate = UtilsTime.validate(time_str, bounds);
            if (formatDate != null) {
                xsdDate = UtilsTime.convertStringoXSDString(formatDate);
                System.out.println("xsdDate->" + xsdDate);
            } else {
                xsdDate = "Unknown-Format";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
      
        return xsdDate;
       
    }
}
