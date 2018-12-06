# Logback appender for LogSense

The project includes a simple Logback appender that pushes data to LogSense using
 [logback-more-appenders](https://github.com/sndyuk/logback-more-appenders) 
 and [Fluency](https://github.com/komamitsu/fluency) underneath.
 
 # Setup
 
 ## Using `logback.xml` 
 
 ### Step 1 - Add LogSense-Logback to your dependencies
 
 In case you are using Maven, this typically means a dependency like:
 
 ```
<dependency>
  <groupId>com.logsense</groupId>
  <artifactId>logback-logsense</artifactId>
  <version>1.0</version>
</dependency>
 ```
  
### Step 2 - Setup Logback appender
 
 Create `src/resources/logback.xml` in your project and put following data:
 
 ```xml
 <?xml version="1.0" encoding="UTF-8" ?>
 <!DOCTYPE logback>
 <configuration>
 
     <!-- LogSense appender. Use the correct accessToken value, as provided by the LogSense app -->
     <appender name="LOGSENSE" class="com.logsense.logback.Appender" >
         <remoteHost>logs.logsense.com</remoteHost>
         <csCustomerToken>YOUR_CS_CUSTOMER_TOKEN</csCustomerToken>
         <!--<useLocalIpAddress>true</useLocalIpAddress>-->
         <!--<csSourceIp>10.12.1.1</csSourceIp>-->
         <!--<csPatternKey>message</csPatternKey>-->
         <!--<sourceName>some name</sourceName>-->
     </appender>
 
    <!-- This is just a standard STDOUT appender - keep it (and others) if you intend to use those -->
     <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
         <encoder>
             <pattern><![CDATA[%date{HH:mm:ss.SSS} [%thread] %-5level %logger{15}#%line %X{req.requestURI} %msg\n]]></pattern>
         </encoder>
     </appender>
 
     <root>
         <level value="DEBUG" />
         <appender-ref ref="STDOUT" />
         <appender-ref ref="LOGSENSE" />
     </root>
 
 </configuration>
```

There are several optional settings:
* `useLocalIpAddress` - when set to true, the local IP address is determined and
sent as the log source IP 
* `csSourceIp` - any address can be put here which will override any other method of 
determining the log source IP
* `csPatternKey` - set to `message` by default; the provided key of structured log is used for 
LogSense automatic pattern recognition
* `sourceName` - not set by default; if provided, adds "source_name" field with the entered value
