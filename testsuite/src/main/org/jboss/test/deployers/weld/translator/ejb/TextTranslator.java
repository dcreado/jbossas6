package org.jboss.test.deployers.weld.translator.ejb;


import java.io.Serializable;

import javax.ejb.EJB;
import javax.inject.Inject;

public class TextTranslator implements Serializable
{ 
   private SentenceParser sentenceParser; 
   
   @EJB Translator translator;
   
   @Inject
   TextTranslator(SentenceParser sentenceParser) 
   { 
      this.sentenceParser = sentenceParser;  
   }
   
   public String translate(String text) 
   { 
      StringBuilder sb = new StringBuilder(); 
      for (String sentence: sentenceParser.parse(text)) 
      { 
         sb.append(translator.translate(sentence)).append(". "); 
      } 
      return sb.toString().trim(); 
   }
   
}
