<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0"
		name="aop">

	<profile name="aop-deployers">
		<profile-source>
			<source>${jboss.server.home.url}deployers</source>	
		</profile-source>
		<deployment>jboss-aop-jboss5.deployer</deployment>
	</profile>
	
</profiles>