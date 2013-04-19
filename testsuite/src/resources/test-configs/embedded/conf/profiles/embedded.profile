<?xml version="1.0" encoding="UTF-8"?>
<profiles xmlns="urn:jboss:profileservice:profiles:1.0">

	<!-- define the embedded profile -->
	<profile name="embedded">
		<!-- A immutable profile source -->
		<profile-source>
			<source>${jboss.server.home.url}deploy</source>	
		</profile-source>
		
		<!-- the required sub-profiles -->
		<sub-profile>jmx</sub-profile>
		<sub-profile>aop</sub-profile>
		
		<!-- jca -->
		<sub-profile>jca</sub-profile>
		
		<!-- ejb3 -->
		<sub-profile>ejb3</sub-profile>
		
		<!-- jboss web -->
		<sub-profile>jboss-web</sub-profile>
		
		<!-- the embedded deployments -->
		<deployment>jmx-console.war</deployment>
		<deployment>ROOT.war</deployment>
	</profile>

</profiles>