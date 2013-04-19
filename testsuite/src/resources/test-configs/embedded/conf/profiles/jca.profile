<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0"
		name="jca">

	<profile name="jca-deployers">
		<profile-source>
			<source>${jboss.server.home.url}deployers</source>	
		</profile-source>
		<deployment>jboss-jca.deployer</deployment>
	</profile>
	
	<profile name="jca-ds">
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		
		<sub-profile>jta</sub-profile>
		
		<deployment>jboss-local-jdbc.rar</deployment>
		<deployment>jboss-xa-jdbc.rar</deployment>
	</profile>
	
	<profile name="jca-runtime">
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		
		<sub-profile>security</sub-profile>
		
		<deployment>hsqldb-ds.xml</deployment>
		<deployment>jca-jboss-beans.xml</deployment>
	</profile>

</profiles>