<p align="center">
  <img src="/public/image/smockin_logo.png" width=400 />
</p>

<p align="center">
  version 2.18.2
</p>

<br />

### Dynamic API, S3 Bucket & Mail Server mocking for application development & QA testing

   - Visit us: https://www.smockin.com

   - Contact us or follow us on Twitter: <b>@smockin_com</b>
   
   - User Guide: https://www.smockin.com/help

<br />
<br />


### OVERVIEW

sMockin is a development tool used to dynamically mock API endpoints, S3 buckets & Email Accounts.

Featuring a rich UI an built in mock servers, creating and managing mocks can be done quickly, both with or without code.

Whether you are a developer who needs to simulate RESTFul API endpoints or an engineer working with complex microservice infrastructure, sMockin can help by mimicking any services that are either unavailable or otherwise too difficult or time consuming to set up.

sMockin runs as a small web app which can be either installed locally onto a personal machine or hosted centrally and used by multiple users.

<br/>

![dashboard](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/dashboard.png)

![client call](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/client-call.png)

<br/>

### KEY FEATURES

* Create dynamic API mocks to mimic real world application behaviour.
* Create & manage S3 Bucket mocks where an AWS account may not be available.
* (NEW) Create & manage mock mail inboxes.
* Run sMockin centrally and create user accounts for your team.
* Import / Export mocks to share between your team & version control.
* Monitor and log traffic going to the HTTP or S3 mock servers.
* Choose whether to build your API mocks using JavaScript or without code.
* A rich and complete UI solution.


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

You can also check out the latest help guide [here](https://www.smockin.com/help)

<br/>

### TROUBLESHOOTING

Please consult the [Configuration & Troubleshooting](https://github.com/mgtechsoftware/smockin/wiki/Configuration-&-Troubleshooting) guide.

<br/>

### LICENCE


sMockin is licensed in accordance with the terms of the Apache License, Version 2.0.

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
        Code Mirror                 -       https://codemirror.net/
        S3proxy                     -       https://github.com/gaul/s3proxy
        GreenMail                   -       https://greenmail-mail-test.github.io/greenmail/


<br/>
<br />


### RECENT RELEASE FEATURES

<br />

**New in version 2.18.0**

- Introducing the Email Mock Server!

1. Swap in the sMockin mock mail server (running on port 8003) as your application's delivery mail server.
2. Create Email accounts in the sMockin UI.
3. View all received email messages from the UI (including mail attachments).

Mail messages can be stored in-memory or saved to database.


<br />
<br />


**New in version 2.17.0**

- Introducing S3 bucket mocking!

Simply create a mock bucket, build your S3 content and point your application to the S3 mock server.

Featuring different synchronisation modes, developing and testing around S3 has never been easier.

Once you've created your first bucket, this can be accessed from port 8002 using any S3 client like so:

![s3 client](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/s3-client.png)

<br />
<br />


**New in version 2.11.0**

- Introducing Block, mock and Swap!

With a number of users discovering sMockin as a great way to monitor live dev traffic, you can now intercept and manipulate on the fly, any requests via the live logging tool.

Combined with the proxy server, this now gives developers and QA engineers, complete visibility and control over all HTTP traffic running between your applications.

<br />
<br />


**New in version 2.10.0**

- The sMockin proxy server now supports multiple url destinations which can be mapped to specific paths.

For example, say you are running 3 independent microservices for each of the following services:

```
/v1/product   -> localhost:9001
/v1/customer  -> localhost:9002
/v1/address   -> localhost:9003
```

You can now add these 'Path to URL' mappings to sMockin and then decide whether swap out any traffic between these services and the end user with mocked responses using various strategies.

This can be highly useful for either temporarily modifying an existing API or adding (mocking) an entirely new endpoint.

<br />
<br />



### ABOUT

sMockin is designed and actively maintained by Matthew Gallina.

Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name actually came about more in relation to a classic Jim Carey movie quote 'Smokin'.

...and yes, far too much thought and time went into it.
