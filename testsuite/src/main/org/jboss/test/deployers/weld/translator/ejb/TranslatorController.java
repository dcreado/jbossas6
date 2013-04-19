package org.jboss.test.deployers.weld.translator.ejb;


public interface TranslatorController
{
   
   public String getText();
   
   public void setText(String text);
   
   public void translate();
   
   public String getTranslatedText();
   
   public void remove();
   
}
