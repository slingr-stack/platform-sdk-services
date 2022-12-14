# platform-services

How to deploy ```service-java-sdk```

1. Setup /.m2/settings.xml
```<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
   <servers>
   <server>
   <id>slingrRepo.write.snapshot</id>
   <username>...</username>
   <password>...</password>
   </server>
   </servers>
   </settings>
   ```

2. Run ```mvn clean deploy```