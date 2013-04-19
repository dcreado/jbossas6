<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0"
		name="security">
		
	<profile name="security-deployers">
		<profile-source>
			<source>${jboss.server.home.url}deployers</source>	
		</profile-source>
		<deployment>security-deployer-jboss-beans.xml</deployment>
	</profile>
	
	<profile name="security-runtime">
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		<deployment>security/security-jboss-beans.xml</deployment>
		<deployment>security/security-policies-jboss-beans.xml</deployment>
	</profile>
		
</profiles>