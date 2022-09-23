import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Play;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import spark.Spark;

import java.io.*;
import java.util.Properties;

import static spark.Spark.before;
import static spark.Spark.options;

public class sampleWebhook {
    public static void main(String[] args) {

        enableCORS("*", "*", "*");

        Spark.get("/notify-winner", (req, res) -> {

            Dotenv dotenv = Dotenv.load();
            String ACCOUNT_SID = dotenv.get("ACCOUNT_SID");
            String AUTH_TOKEN = dotenv.get("AUTH_TOKEN");
            String TWILIO_PHONE = dotenv.get("TWILIO_PHONE");
            String TEST_PHONE = dotenv.get("MY_PHONE");

            Properties prop = new Properties();
            try (InputStream input = new FileInputStream("src/main/resources/" + "config.properties")) {
                // load a properties file
                prop.load(input);

            } catch (IOException ex) {
                ex.printStackTrace();
            }

            String winnerPhone = req.headers("winner");
            if (prop.getProperty("test").toUpperCase().equals("YES")) winnerPhone = TEST_PHONE;
            String winnerMessage = prop.getProperty("sender.message");

            Twilio.init(ACCOUNT_SID,AUTH_TOKEN);

            com.twilio.rest.api.v2010.account.Message.creator(
                    new PhoneNumber(winnerPhone),
                    new PhoneNumber(TWILIO_PHONE),
                    winnerMessage).create();

            Message.creator(
                    new PhoneNumber("whatsapp:"+winnerPhone),
                    new PhoneNumber("whatsapp:"+TWILIO_PHONE),
                    winnerMessage).create();

            Say sayES = new Say.Builder(winnerMessage).voice(Say.Voice.POLLY_ENRIQUE).loop(4).language(Say.Language.ES_ES).build();
            Play play = new Play.Builder("http://demo.twilio.com/docs/classic.mp3").build();
            VoiceResponse response = new VoiceResponse.Builder().say(sayES).play(play).build();

            Call.creator(
                    new PhoneNumber(winnerPhone),
                    new PhoneNumber(TWILIO_PHONE),
                    new Twiml(response.toXml())
            ).create();

            return  "{result:'Success'}";
        });

        Spark.get("/participants", (req, res) -> {

            res.type("application/json");

            //Read Properties
            Properties prop = new Properties();
            try (InputStream input = new FileInputStream("src/main/resources/" + "config.properties")) {
                // load a properties file
                prop.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            JSONObject jsonObject = null;

            try (Reader reader = new FileReader(prop.getProperty("sender.file.path"))) {
                jsonObject = (JSONObject) parser.parse(reader);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return jsonObject.toJSONString();
        });

    }

    // Enables CORS on requests. This method is an initialization method and should be called once.
    private static void enableCORS(final String origin, final String methods, final String headers) {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Request-Method", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: this may or may not be necessary in your particular application
            response.type("application/json");
        });
    }

}
