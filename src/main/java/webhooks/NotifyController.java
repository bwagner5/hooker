package webhooks;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/notify")
public class NotifyController {

    private static final Logger log = LoggerFactory.getLogger(NotifyController.class);

    private final String from;
    private final String blackListDomain;

    @Autowired
    public NotifyController(Config config) {
        this.from = config.getEmailNotificationFrom();
        this.blackListDomain = config.getBlackListDomain();
    }


    @RequestMapping(value = "email", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public String email(HttpServletRequest request, @RequestBody Map<String, Object> json){

        log.info("Email Request: " + json.toString());

        String subject = (String) json.getOrDefault("subject", "");
        String greeting = (String) json.getOrDefault("greeting", "");
        String preheader = (String) json.getOrDefault("preheader", "");
        String message = (String) json.getOrDefault("message", "");
        String linkUrl = (String) json.getOrDefault("linkUrl", "");
        String linkText = (String) json.getOrDefault("linkText", "");

        List<String> to = (List) json.getOrDefault("to", new ArrayList<String>());

        this.sendEmail(to, subject, greeting, preheader, message, linkUrl, linkText);

        return "";

    }

    private void sendEmail(List<String> to, String subject, String greeting, String preheader, String message, String linkUrl, String linkText){

        if(!to.stream().allMatch(email->email.endsWith(this.blackListDomain))){
            log.error("Not sending email to blacklisted addresses.");
            return;
        }

        if(to.size() <= 0){
            log.error("Destination address list is empty, not sending email");
            return;
        }

        String emailHtml = EmailTemplate.generateEmailHtml(greeting, preheader, message, linkUrl, linkText);

        Destination destinations = new Destination().withToAddresses(to);
        Content subjectContent = new Content().withData(subject);
        Content htmlBody = new Content().withData(emailHtml);
        Body body = new Body().withHtml(htmlBody);

        Message emailMessage = new Message().withSubject(subjectContent).withBody(body);

        SendEmailRequest request = new SendEmailRequest().withSource(this.from).withDestination(destinations).withMessage(emailMessage);

        try {
            AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.defaultClient();
            client.sendEmail(request);
            log.info("Email was sent to: " + to.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("There was a problem sending an email to the following recipients: " + to.toString());
        }
    }


}
