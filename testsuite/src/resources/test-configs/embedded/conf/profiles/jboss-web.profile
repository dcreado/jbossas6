<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0"
		name="jboss-web">

	<profile name="jboss-web-deployers">
		<profile-source>
			<source>${jboss.server.home.url}deployers</source>	
		</profile-source>
		
		<sub-profile>jboss-metadata</sub-profile>
		
		<deployment>jbossweb.deployer</deployment>
	</profile>

	<profile name="jboss-web-runtime">
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		
		<deployment>jbossweb.sar</deployment>
	</profile>
	
</profiles>