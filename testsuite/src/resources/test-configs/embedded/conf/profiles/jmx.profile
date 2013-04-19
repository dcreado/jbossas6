<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0">
		
	<profile name="jmx">
		<!-- A immutable profile source -->
		<profile-source>
			<source>${jboss.server.home.url}conf</source>	
		</profile-source>
		<deployment>jboss-service.xml</deployment>
	</profile>
		
</profiles>