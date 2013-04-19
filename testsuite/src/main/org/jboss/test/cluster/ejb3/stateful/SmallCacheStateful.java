package org.jboss.test.cluster.ejb3.stateful;

import javax.ejb.Remote;

@Remote
public interface SmallCacheStateful
{
   public void setId(int id);

   public int doit(int id);

}