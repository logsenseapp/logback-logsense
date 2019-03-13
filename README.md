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
  <version>1.1.0</version>
</dependency>
```
  
#### Using `log4j` instead of logback

If your project is already using `log4j`, a migration dependency can be simply added:
```

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>log4j-over-slf4j</artifactId>
	<version>1.7.25</version>
</dependency>

```
  
### Step 2 - Setup Logback appender
 
 Create `src/resources/logback.xml` in your project and put following data:
 
 ```xml
 <?xml version="1.0" encoding="UTF-8" ?>
 <configuration>
 
     <!-- LogSense appender. Use the correct accessToken value, as provided by the LogSense app -->
     <appender name="LOGSENSE" class="com.logsense.logback.Appender" >
         <logsenseToken>YOUR_LOGSENSE_TOKEN</logsenseToken>
         <!--<remoteHost>logs.logsense.com</remoteHost>-->
         <!--<useLocalIpAddress>true</useLocalIpAddress>-->
         <!--<sourceIp>10.12.1.1</sourceIp>-->
         <!--<patternKey>message</patternKey>-->
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
* `sourceIp` - any address can be put here which will override any other method of 
determining the log source IP
* `patternKey` - set to `message` by default; the provided key of structured log is used for 
LogSense automatic pattern recognition
* `sourceName` - not set by default; if provided, adds "source_name" field with the entered value

#### Alternative methods of providing the token

The token could be also provided using environment variable, e.g.:

```
$ LOGSENSE_TOKEN=aaa-111-bbb-222 java ....
```
or via property, e.g.

```
$ java -Dlogsense.token=aaa-111-bbb-222 ...
``` 