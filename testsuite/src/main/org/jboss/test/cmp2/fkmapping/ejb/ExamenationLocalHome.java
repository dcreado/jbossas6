/*
 * Generated by XDoclet - Do not edit!
 */
package org.jboss.test.cmp2.fkmapping.ejb;

/**
 * Local home interface for Examenation.
 */
public interface ExamenationLocalHome
   extends javax.ejb.EJBLocalHome
{
   public static final String COMP_NAME="java:comp/env/ejb/ExamenationLocal";
   public static final String JNDI_NAME="Examenation";

   public org.jboss.test.cmp2.fkmapping.ejb.ExamenationLocal create(java.lang.String examId , java.lang.String subject , java.lang.String deptCode , long groupNum)
      throws javax.ejb.CreateException;

   public org.jboss.test.cmp2.fkmapping.ejb.ExamenationLocal findByPrimaryKey(org.jboss.test.cmp2.fkmapping.ejb.ExamenationPK pk)
      throws javax.ejb.FinderException;

}