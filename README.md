# Hooker

Hooker is a cleveryly (at least I think so) named application which allows you to accept webhooks from public services like Github and take secure actions based off of those hooks. 


# Usage

Hooker needs some love, but it still runs in an opinonated fashion, just not too configurable yet! 

`./gradlew bootrun`

That command will start the spring boot application on port 8080 (or whatever you configure it to be, yep, that port number is configurable, just not much else ;) 

# Use-Cases

#### Microservices w/ Jenkins and Github

You have a central git repository for your microservices and want to trigger Jenkins when commits come in with a webhook (polling is gross). 
But you ONLY want to build the services that were modified! Hooker comes to the rescue. Hooker can parse the github file changes from the commit or pull request, and only trigger the Jenkins pipelines that match.
Further, Hooker is the public facing service for the webhook, not Jenkins! Jenkins should NEVER be exposed publicly! 
