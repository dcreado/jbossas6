package org.jboss.test.deployers.weld.numberguess;


import java.io.Serializable;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Named;
import javax.inject.Inject;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Instance;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@Named
@SessionScoped
public class Game implements Serializable
{
   private int number;
   private int guess;
   private int smallest;
   
   @MaxNumber @Inject
   private int maxNumber;
   
   private int biggest;
   private int remainingGuesses;
   
   @Random @Inject Instance<Integer> randomNumber;
   private BeanManager beanManager;

   public Game()
   {
   }

   public int getNumber()
   {
      return number;
   }
   
   public int getGuess()
   {
      return guess;
   }
   
   public void setGuess(int guess)
   {
      this.guess = guess;
   }
   
   public int getSmallest()
   {
      return smallest;
   }
   
   public int getBiggest()
   {
      return biggest;
   }
   
   public int getRemainingGuesses()
   {
      return remainingGuesses;
   }
   
   public String check() throws InterruptedException
   {
      if (guess>number)
      {
         biggest = guess - 1;
      }
      if (guess<number)
      {
         smallest = guess + 1;
      }
      if (guess == number)
      {
         FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Correct!"));
      }
      remainingGuesses--;
      return null;
   }
   
   @PostConstruct
   public void reset()
   {
      try
      {
         InitialContext context = new InitialContext();
         beanManager = (BeanManager)context.lookup("java:comp/BeanManager");
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
      this.smallest = 0;
      this.guess = 0;
      this.remainingGuesses = 10;
      this.biggest = maxNumber;
      this.number = randomNumber.get();
   }
   
   public void validateNumberRange(FacesContext context,  UIComponent toValidate, Object value)
   {
      int input = (Integer) value;

      if (remainingGuesses <= 0)
      {
     	FacesMessage message = new FacesMessage("No guesses left!");
     	context.addMessage(toValidate.getClientId(context), message);
     	((UIInput)toValidate).setValid(false);
     	return;
      }

      if (input < smallest || input > biggest) 
      {
	((UIInput)toValidate).setValid(false);

	FacesMessage message = new FacesMessage("Invalid guess");
	context.addMessage(toValidate.getClientId(context), message);
      }
   }
}
