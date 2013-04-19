package org.jboss.test.deployers.weld.translator.ejb;

import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.inject.Inject;

@Stateful
@RequestScoped
@Named("translator")
public class TranslatorControllerBean implements TranslatorController
{
   
   @Inject TextTranslator translator;
   
   private String inputText;
   
   private String translatedText;
   
   public String getText()
   {
      return inputText;
   }
   
   public void setText(String text)
   {
      this.inputText = text;
   }
   
   public void translate()
   {
      translatedText = translator.translate(inputText);
   }
   
   public String getTranslatedText()
   {
      return translatedText;
   }
   
   @Remove
   public void remove()
   {
      
   }
   
}
