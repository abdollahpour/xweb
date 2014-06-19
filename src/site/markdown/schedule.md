You can set schedule to run module with specific parameters once or periodically at a specific time. All the modules can have schedule with different parameters.

```java
<module>
	...
	<schedules>
		<schedule start="START" period="PERIOD" query="PARAMS"/>
		...
	</schedules>
</module>
```

###START

Start time for scheduling.


###PERIOD

Time between each repeat. Leave it empty if you want to run the task just one time.


###Query

Query string that will pass to module. Ex: "param1=value1&param2=value2", but remember you should escape & with &amp; string, so it will be: "param1=value1&amp;param2=value2"


###Time format

Supported time format for START and PERIOD attributes:
Second: 23s / 23sec / 23second / 23seconds
Minuet: 21 / 21m / 22min / 22minuet / 22minuets
Hour: 2 / 2h / 2hour / 2hours
Hour+minuet: 10:34 / 22:14
Hour+minuet+second: 10:34:30 / 22:14:15
Day: 3d / 3day / 3days
Month: 6month / 6months
Year: 2y / 2year / 2years
Date: 2014/11/1 23:11:00


##Example

```xml
<module>
	...
	<schedules>
		<schedule start="10" query="param1=value1"/>
		<schedule start="Fri 9:00" period="week" query="param1=value1&amp;param2=value"/>
		<schedule start="11pm" period="2day" query="param1=value1"/>
		...
	</schedules>
</module>
```


###Tips

* Minimum period is 1 second.
* No security or roll will apply to schedule request (Fake request send to module).
* With ```getRemoteUser().equals("127.0.0.1")``` you can identify scheduled requests inside of the module.
* If you are using heavy tasks or loop inside of the scheduler, remember that thread will interrupted after application stop or undeploy on the server. 