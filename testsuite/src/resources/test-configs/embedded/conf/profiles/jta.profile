<?xml version="1.0" encoding="UTF-8"?>
<profiles
		xmlns="urn:jboss:profileservice:profiles:1.0"
		name="jta">
		
	<profile name="transaction-manager">
		<!-- A immutable profile source -->
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		<deployment>transaction-jboss-beans.xml</deployment>
	</profile>
		
</profiles>