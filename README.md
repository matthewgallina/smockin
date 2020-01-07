<p align="center">
  <img src="/public/image/smockin_logo.png" width=400 />
</p>

<p align="center">
  version 2.1.0
</p>

### Dynamic REST API simulation and for application development & QA testing

   - Visit us: https://www.smockin.com

   - Contact: info@smockin.com

<br />

**Version 2.1.0 of sMockin brings about a few significant changes...

- The JMS and FTP mocking servers have been removed (as no one was using them) and we are now purely focused on HTTP based API mocking.

- Adding and editing mocks no longer requires the mock server to be continuously restarted, providing a much more seamless experience.

- Multi user mode has been greatly improved based on user feedback and suggestions.

- Users looking for more control can now utilise javascript to deliver mock functionality.

<br />

### OVERVIEW

sMockin is an API mocking tool used to dynamically simulate HTTP endpoints.

Featuring a rich UI and an inbuilt HTTP mock server, mocking your existing or new API model can be done quickly and without any coding or scripting.

Whether you are a developer who needs to simulate RESTFul endpoints or an engineer working with complex SOA / IoT / microservice infrastructure, sMockin can help by mimicking any services that are either unavailable or otherwise too difficult or time consuming to set up.

sMockin runs as a small web app which can be either installed locally onto a personal machine or hosted centrally and used by multiple users.

<br/>

![dashboard](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/dashboard.png)

![client call](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/client-call.png)

<br/>

### KEY FEATURES

* Create dynamic API mocks to mimic real world application behaviour.
* Run sMockin centrally and create user accounts for your team.
* Import / Export mocks to share between your team & version control.
* Monitor and log traffic going to the HTTP mock server.
* A complete UI solution requiring zero coding.


<br/>

### REQUIREMENTS

   - Java 8
   - Maven 3

   Please Note
      -   All bash scripts were written and tested on GNU Bash version 3.2.57(1)-release.
      -   All BAT files were were written and tested on Windows 7.

<br/>

### QUICK START

Clone this repo:

```
git clone https://github.com/mgtechsoftware/smockin.git
```

Change the current directory to **smockin** and build the project:

```
cd smockin

mvn clean install
```

Run the **install** and **start** scripts (for windows, use the equivalent .bat files):

```
./install.sh

./start.sh
```

Finally from your browser open the dashboard:

```
http://localhost:8000/index.html
```

(If running sMockin for the first time then please allow 20 - 30 secs for the app to fully start up.)

<br />

For further details, please consult the [Installation](https://github.com/mgtechsoftware/smockin/wiki/Installation) and [Getting Started](https://github.com/mgtechsoftware/smockin/wiki/API-Mock-Tutorial:-Getting-Started) guides in the Wiki.

<br/>

### TROUBLESHOOTING

Please consult the [Configuration & Troubleshooting](https://github.com/mgtechsoftware/smockin/wiki/Configuration-&-Troubleshooting) guide.

<br/>

### LICENCE


sMockin is licensed according to the terms of the Apache License, Version 2.0.

The full text of this license can be found at https://www.apache.org/licenses/LICENSE-2.0

<br/>

### ACKNOWLEDGEMENTS / THIRD PARTIES

   sMockin is built upon the following open source frameworks:

        Spring Boot                 -       https://projects.spring.io/spring-boot
        Hibernate                   -       http://hibernate.org
        Spark                       -       http://sparkjava.com
        Maven                       -       https://maven.apache.org
        AngularJS                   -       https://angularjs.org
        UI Bootstrap                -       https://angular-ui.github.io/bootstrap
        vkBeautify                  -       https://github.com/vkiryukhin/vkBeautify
        H2                          -       http://www.h2database.com
        HikariCP                    -       https://brettwooldridge.github.io/HikariCP
        Apache Commons              -       https://commons.apache.org/
        Apache Http Components      -       https://hc.apache.org/
        Apache Commons IO           -       https://commons.apache.org/proper/commons-io/
        JUnit                       -       http://junit.org
        Mockito                     -       http://site.mockito.org/
        Apache ActiveMQ             -       http://activemq.apache.org/
        Apache FtpServer            -       https://mina.apache.org/ftpserver-project/
        RAML Parser 2               -       https://github.com/raml-org/raml-java-parser
        Jasypt                      -       http://www.jasypt.org/
        Java-JWT                    -       https://github.com/auth0/java-jwt
        JWT-Decode                  -       https://github.com/auth0/jwt-decode
        JQuery                      -       https://jquery.com/

<br/>

### ABOUT

sMockin is designed and actively maintained by MG Tech Software Ltd.

Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name actually came about more in relation to a classic Jim Carey movie quote 'Smokin'. We then added the extra letter to play on the concept of software mocking and there you have it. 

Yes, way too much thought went into that...
