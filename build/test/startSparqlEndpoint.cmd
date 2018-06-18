@echo off
REM arguments are path to fedxConfig and port
java -Xmx1024m -Dlog4j.configuration=file:config\log4j-sparql.properties -cp bin;lib\*;lib\jetty7\* com.fluidops.fedx.server.Start config/fedxConfig.prop 80
exit