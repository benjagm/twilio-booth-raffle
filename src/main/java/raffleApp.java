import com.twilio.Twilio;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.twilio.type.PhoneNumberPrice;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class raffleApp {
    public static void main(String[] args) {

        //Read properties
        Dotenv dotenv = Dotenv.load();
        String ACCOUNT_SID = dotenv.get("ACCOUNT_SID");
        String AUTH_TOKEN = dotenv.get("AUTH_TOKEN");
        String MY_PHONE = dotenv.get("MY_PHONE");
        String TWILIO_PHONE = dotenv.get("TWILIO_PHONE");

        //Init Twilio
        Twilio.init(ACCOUNT_SID,AUTH_TOKEN);
        Properties prop = new Properties();

        //Read Properties
        try (InputStream input = new FileInputStream("src/main/resources/" + "config.properties")) {

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            System.out.println(prop.getProperty("event.name"));
            System.out.println(prop.getProperty("event.year"));
            System.out.println(prop.getProperty("event.time.before"));
            System.out.println(prop.getProperty("event.time.after"));
            System.out.println(prop.getProperty("sender.blacklist"));
            System.out.println(prop.getProperty("sender.whitelist"));
            System.out.println(prop.getProperty("sender.message"));
            System.out.println(prop.getProperty("sender.file.path"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //Get phone numbers from Twilio
        var messages = Message.reader().setTo(TWILIO_PHONE).
                setDateSentBefore(ZonedDateTime.parse(prop.getProperty("event.time.before"))).
                setDateSentAfter(ZonedDateTime.parse(prop.getProperty("event.time.after"))).read();

        HashSet<PhoneNumber> phoneNumbers = new HashSet<>();
        messages.forEach(msg -> phoneNumbers.add(msg.getFrom()));

        if (prop.getProperty("sender.blacklist").length()>0) {
            List<String> blacklist =
                    Stream.of(prop.getProperty("sender.blacklist").split(","))
                            .collect(Collectors.toList());

            for (int i = 0; i < blacklist.size(); i++) {
                PhoneNumber newPhone = new PhoneNumber(blacklist.get(i));
                phoneNumbers.remove(newPhone);
            }
        }

        if (prop.getProperty("sender.whitelist").length()>0) {
            List<String> whitelist =
                    Stream.of(prop.getProperty("sender.whitelist").split(","))
                            .collect(Collectors.toList());

            for (int i = 0; i < whitelist.size(); i++) {
                PhoneNumber newPhone = new PhoneNumber(whitelist.get(i));
                phoneNumbers.add(newPhone);
            }
        }

        //Create json file with results for front-end to consume
        JSONObject participants = new JSONObject();
        participants.put("Event", prop.getProperty("event.name"));
        participants.put("Year", prop.getProperty("event.year"));
        participants.put("TwilioPhone", TWILIO_PHONE);
        participants.put("generationDate", getDateTime());

        JSONArray list = new JSONArray();

        int i = 0;
        for(PhoneNumber participantNumber : phoneNumbers) {
            //Remove black list items
            JSONObject mobj = new JSONObject();
            mobj.put("id", ++i);
            mobj.put("phoneNumber", participantNumber.toString());
            mobj.put("anonymousValue", getAnonymousValue(i,participantNumber));
            list.add(mobj);
        }

        participants.put("numbers", list);

        try (FileWriter file = new FileWriter(prop.getProperty("sender.file.path"))) {
            file.write(participants.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private  final static String getDateTime()
    {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        return df.format(new Date());
    }

    private static String getAnonymousValue(int i, PhoneNumber phoneNumber)
    {
        String value = new String();
        value = phoneNumber.toString().substring(0, 4) + "******" + phoneNumber.toString().substring(phoneNumber.toString().length() - 3) + "#" + String.valueOf(i) ;
        return value;
    }

}

