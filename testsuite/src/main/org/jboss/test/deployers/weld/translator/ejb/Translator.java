package org.jboss.test.deployers.weld.translator.ejb;


import javax.ejb.Local;

@Local 
public interface Translator 
{ 

   public String translate(String sentence);
   
}
