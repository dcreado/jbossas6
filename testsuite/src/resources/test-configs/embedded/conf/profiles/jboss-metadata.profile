<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0">

	<profile name="jboss-metadata">
		<profile-source>
			<source>${jboss.server.home.url}deployers</source>	
		</profile-source>
		<deployment>metadata-deployer-jboss-beans.xml</deployment>
	</profile>
	
</profiles>