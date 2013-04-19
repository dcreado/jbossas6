/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/ 
package org.jboss.test.deployers.support.deployer;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.weld.integration.deployer.DeployersUtils;
import org.jboss.weld.integration.deployer.env.AbstractBootstrapInfoDeployer;
import org.jboss.weld.integration.deployer.env.BootstrapInfo;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MockValidatorFactoryDeployer extends AbstractBootstrapInfoDeployer
{
   public MockValidatorFactoryDeployer()
   {
      super(true);
      addOutput(DeployersUtils.JAVAX_VALIDATION_VALIDATOR_FACTORY);
   }

   @Override
   protected void deployInternal(DeploymentUnit unit, BootstrapInfo info) throws DeploymentException
   {
      unit.addAttachment(DeployersUtils.JAVAX_VALIDATION_VALIDATOR_FACTORY, MockValidatorFactory.INSTANCE);
   }

   private static class MockValidatorFactory implements ValidatorFactory
   {
      static ValidatorFactory INSTANCE = new MockValidatorFactory();
      
      private static Validator validator = new MockValidator();
      
      private static ValidatorContext context = new MockValidatorContext(validator);
      
      private static MessageInterpolator messageInterpolator = new MockMessageInterpolator();
      
      public MessageInterpolator getMessageInterpolator()
      {
         return messageInterpolator;
      }

      public Validator getValidator()
      {
         return validator;
      }

      public <T> T unwrap(Class<T> type)
      {
         throw new ValidationException("NYI");
      }

      public ValidatorContext usingContext()
      {
         return context;
      }

      public ConstraintValidatorFactory getConstraintValidatorFactory()
      {
         return null;
      }

      public TraversableResolver getTraversableResolver()
      {
         return null;
      }
   }
   
   private static class MockValidatorContext implements ValidatorContext 
   {
      Validator validator;
      
      public MockValidatorContext(Validator validator)
      {
         this.validator = validator;
      }
      public Validator getValidator()
      {
         return validator;
      }

      public ValidatorContext messageInterpolator(MessageInterpolator messageInterpolator)
      {
         return this;
      }

      public ValidatorContext traversableResolver(TraversableResolver traversableResolver)
      {
         return this;
      }
      public ValidatorContext constraintValidatorFactory(ConstraintValidatorFactory factory)
      {
         return this;
      }
   }
   
   private static class MockValidator implements Validator
   {
      public BeanDescriptor getConstraintsForClass(Class<?> clazz)
      {
         throw new ValidationException("NYI");
      }

      public <T> T unwrap(Class<T> type)
      {
         throw new ValidationException("NYI");
      }

      public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups)
      {
         return Collections.emptySet();
      }

      public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups)
      {
         return Collections.emptySet();
      }

      public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value,
            Class<?>... groups)
      {
         return Collections.emptySet();
      }
   }
   
   private static class MockMessageInterpolator implements MessageInterpolator
   {

      public String interpolate(String messageTemplate, Context context)
      {
         return messageTemplate;
      }

      public String interpolate(String messageTemplate, Context context, Locale locale)
      {
         return messageTemplate;
      }
      
   }
}
