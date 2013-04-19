package org.jboss.test.deployers.weld.translator.ejb;


import javax.ejb.Stateless;

@Stateless
public class SentenceTranslator implements Translator 
{ 
   
   public String translate(String sentence) 
   { 
      return "Lorem ipsum dolor sit amet"; 
   }
   
}
