# XWeb

XWeb is simple modular library to create JavaEE base web applications. You are able to authenticate, restrict, validate, compress and do many other cool things just with simple XML file (xweb.xml). You can use XWeb for simple project without even one line of Java code! But for more feature like connect it to database, add advance user functionality and more, you need to simply extends some modules, or you write your own module.

## Use XWeb with maven
If you are using maven, you need to add our own repository to repositories list:

```
<repositories>
	<repository>
		<id>xweb</id>
		<url>https://raw.github.com/abdollahpour/xweb/master/releases</url>
	</repository>
</repositories>
```

Add maven dependency

```
<properties>
	<ir.xweb.version>0.7.0-SNAPSHOT</ir.xweb.version>
</properties>
<dependencies>
	<dependency>
		<groupId>ir.xweb</groupId>
		<artifactId>xweb-wiki</artifactId>
		<version>${ir.xweb.version}</version>
	</dependency>
	<dependency>
		<groupId>ir.xweb</groupId>
		<artifactId>xweb</artifactId>
		<version>${ir.xweb.version}</version>
		<type>test-jar</type>
		<scope>test</scope>
	</dependency>
</dependencies>
```


## Configurations
Add to web.xml

```
<filter>
	<filter-name>XWebFilter</filter-name>
	<filter-class>ir.xweb.server.XWebFilter</filter-class>
</filter>
<filter-mapping>
	<filter-name>XWebFilter</filter-name>
	<url-pattern>/*</url-pattern>
</filter-mapping>
<listener>
	<listener-class>ir.xweb.server.XWebListener</listener-class>
</listener>
<servlet>
	<servlet-name>api</servlet-name>
	<display-name>api</display-name>
	<description>Handle system API</description>
	<servlet-class>ir.xweb.server.XWebServlet</servlet-class>
	<load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
	<servlet-name>api</servlet-name>
	<url-pattern>/api</url-pattern>
</servlet-mapping>
```

Create xweb.xml (same directory as web.xml)

```
<?xml version="1.0" encoding="utf-8"?>
<xweb version="0.7.0">
	<properties>
		<property>
			...
		</property>
	</properties>
	<modules>
		<module>
			...
		</module>
		...
	</modules>
</xweb>
```

## How to define a module
You should put &lt;module&gt; in &lt;modules&gt; tag in xweb.xml file like this:

```
<module>
	<name>module_name</name>
	<author>author_name</author>
	<class>full.path.of.module.classname</class>
	<validators>
		<validator require="true|false" param="param_name" regex="regex_matcher" />
	</validators>
	<roles>
		<role param="action" eval="validator_script" value="role_match_regex" />
	</roles>
	<properties>
		<property key="key1" value="value1" />
		<property key="key1">value1</property>
	</properties>
	<schedules>
		<schedule start="start_time_delay" unit="minuet|hour|day|month" period="repeat_each" query="key1=value1&amp;key2=value2"/>
	</schedules>
</module>
```

* **&lt;namme&gt;:** The name of modules that should be a-z and 0-9 characters (no space)
* **&lt;author&gt;:** All valid XML characters
* **&lt;class&gt;:** Module full class (name). Remember that if you are using obfuscation tools (like proguard) add follow line to your configuraion:
`-keep public class * extends ir.xweb.test.module.Module`
* **&lt;validator&gt;:** Validate all the parameters that passed to module over http (GET or POST).
 * require: Means that this parameter is mandetory or not (default is false)
 * param: Paremeter that you want to validate
 * regex: Regex that use for matching
* **&lt;role&gt;:** Role checking. You can check that specific role (user role) can access the module with specific parameter or not.
 * name: name of param(s) that you want to check in this role. You can use multi param and seprate them with ',' character.
 * eval: Javascript that will run for validation. The parameter valid replace with %param% keyword in this script. For example, if you have action in params, and eval is 'OK'==%action%, the script that will run will be: 'OK'=='param'. To avoid script injection, all the values pass as string to script part.
 * value: Regex that you want to match for roles. For example "admin|user" apply for both admin and user
* **&lt;property&gt;:** Please read [Properties](wiki/Properties)
* **&lt;schedule&gt;:** Run specific task in specific time or peroid of time.

## Default modules
* [ResourceModule](wiki/ResourceModule)
* [AuthenticationModule](wiki/AuthenticationModule)
* [GzipModule](wiki/GzipModule)
* [RedirectModule](wiki/RedirectModule)
* [RewriteModule](wiki/RewriteModule)
* [LogModule](wiki/LogModule)