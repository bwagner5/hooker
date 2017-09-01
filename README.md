# Hooker

Hooker is a cleveryly (at least I think so) named application which allows you to accept webhooks from public services like Github and take secure actions based off of those hooks. 


# Usage

Hooker needs some love, but it still runs in an opinonated fashion, just not too configurable yet! 

`./gradlew bootrun`

That command will start the spring boot application on port 8090 (or whatever you configure it to be, yep, that port number is configurable, just not much else ;)


# Development

It is useful to have a Jenkins instance running locally to test any Jenkins integrations.

Simple:

`docker run -p 8080:8080 -p 50000:50000 jenkins/jenkins:lts`

Keep State (useful to keep the users and pipelines around to test triggers):

`docker run -p 8080:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home jenkins/jenkins:lts`

Other Jenkins Docker info can be found here: https://github.com/jenkinsci/docker


The default configuration file will use the Jenkins instance on localhost:8080


# Use-Cases

#### Microservices w/ Jenkins and Github

You have a central git repository for your microservices and want to trigger Jenkins when commits come in with a webhook (polling is gross). 
But you ONLY want to build the services that were modified! Hooker comes to the rescue. Hooker can parse the github file changes from the commit or pull request, and only trigger the Jenkins pipelines that match.
Further, Hooker is the public facing service for the webhook, not Jenkins! Jenkins should NEVER be exposed publicly! 

